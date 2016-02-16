package me.maciejb.redisbench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Keep, Sink}
import akka.util.ByteString
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}

import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._

@State(Scope.Benchmark)
class MoreRunsInSingleIteration {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = ExecutionContext.Implicits.global

  val Key = "rb:10Mi"

  val threads: Int = 32

  implicit val redisConn = new RedisConnectivity(StandaloneRedisConfig("localhost", poolSize = threads))

  @Setup
  def setupRedis(): Unit = {
    def newByteStringOfSize(size: Int): ByteString =
      ByteString.fromArray((0 until size).map(i => ((i % (126 - 32)) + 32).toByte).toArray)

    val fut = redisConn.redisExec { client =>
      client.exists(Key).flatMap {
        case true => Future.successful(true)
        case false => client.set(Key, newByteStringOfSize(10 * 1024 * 1024))
      }
    }
    Await.result(fut, 10.seconds)
  }

  @Benchmark
  @OperationsPerInvocation(32 * 4)
  def benchmark(): Unit =
    Await.result(source.toMat(Sink.ignore)(Keep.right).run(), 5.seconds)

  def source =
    Source(0 until threads * 4).mapAsyncUnordered(threads) { _ => redisConn.redisExec(_.get[ByteString](Key)) }

  @TearDown
  def shutdown(): Unit = Await.ready(system.terminate(), 2.minutes)

}

object MoreRunsInSingleIterationApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[MoreRunsInSingleIteration].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .jvmArgs("-Xmx4g")
      .build
    new Runner(opt).run
  }

}
