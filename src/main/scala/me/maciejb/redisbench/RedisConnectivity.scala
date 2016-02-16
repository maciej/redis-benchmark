package me.maciejb.redisbench

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, Config}

import ConfigExtensionMethods._
import redis._
import scala.collection.JavaConverters._
import scala.concurrent.{Future, ExecutionContext}

sealed trait RedisConfig
case class StandaloneRedisConfig(host: String, port: Int = RedisConfig.DefaultRedisPort,
                                 poolSize: Int = RedisConfig.DefaultPoolSize) extends RedisConfig
case class SentinelsRedisConfig(sentinels: Seq[(String, Int)], master: String) extends RedisConfig

object RedisConfig {

  val DefaultRedisPort = 6379
  val DefaultPoolSize = 1

  def build(rootConfig: Config = ConfigFactory.load()): RedisConfig = {
    val config = rootConfig.getConfig("redis")
    if (config.hasPath("sentinels")) {
      def parseSentinels(strSeq: Seq[String]): Seq[(String, Int)] = {
        strSeq.map { str =>
          val arr = str.split(':')
          require(arr.length == 2)
          (arr(0), arr(1).toInt)
        }
      }

      SentinelsRedisConfig(
        parseSentinels(config.getStringList("sentinels").asScala),
        config.getString("master")
      )
    } else {
      StandaloneRedisConfig(
        config.getString("host"),
        config.getIntOr("port", DefaultRedisPort),
        config.getIntOr("pool-size", DefaultPoolSize)
      )
    }
  }


  lazy val Localhost = StandaloneRedisConfig("localhost")

}

class RedisConnectivity(redisConfig: RedisConfig = RedisConfig.build())
                       (implicit ec: ExecutionContext,
                        system: ActorSystem) {

  type RClient = RedisCommands

  private[this] final lazy val redisClient: RClient = redisConfig match {
    case c: StandaloneRedisConfig if c.poolSize == 1 => RedisClient(host = c.host, port = c.port)
    case c: StandaloneRedisConfig => RedisClientPool(for (i <- 0 to c.poolSize) yield RedisServer(c.host, c.port))
    case c: SentinelsRedisConfig => SentinelMonitoredRedisClient(c.sentinels, c.master)
  }

  def redisExec[T](fun: RClient => Future[T]): Future[T] = fun(redisClient)

  def shutdown(): Unit = {
    redisClient.asInstanceOf[ {def stop(): Unit}].stop()
  }

}

