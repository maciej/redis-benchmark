package me.maciejb.redisbench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Keep, Sink}
import akka.util.ByteString
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

class ParallelRedisBenchmark {


}

object ParallelRedisBenchmark {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = ExecutionContext.Implicits.global

  val Key = "rb:10Mi"

  @State(Scope.Benchmark)
  abstract class AbstractBenchmark {

    val threads: Int

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

    def benchmark(): Unit =
      Await.result(source.toMat(Sink.ignore)(Keep.right).run(), 2.seconds)

    def source =
      Source(0 until threads).mapAsyncUnordered(threads) { _ => redisConn.redisExec(_.get[ByteString](Key)) }

    @TearDown
    def shutdown(): Unit = Await.ready(system.terminate(), 2.minutes)
  }

  class Benchmark1 extends AbstractBenchmark {
    override val threads: Int = 1

    @Benchmark
    @OperationsPerInvocation(1)
    override def benchmark() = super.benchmark()

  }

  class Benchmark2 extends AbstractBenchmark {
    override val threads: Int = 2

    @Benchmark
    @OperationsPerInvocation(2)
    override def benchmark() = super.benchmark()
  }

  class Benchmark4 extends AbstractBenchmark {
    override val threads: Int = 4

    @Benchmark
    @OperationsPerInvocation(4)
    override def benchmark() = super.benchmark()
  }

  class Benchmark8 extends AbstractBenchmark {
    override val threads: Int = 8

    @Benchmark
    @OperationsPerInvocation(8)
    override def benchmark() = super.benchmark()
  }

  class Benchmark16 extends AbstractBenchmark {
    override val threads: Int = 16

    @Benchmark
    @OperationsPerInvocation(16)
    override def benchmark() = super.benchmark()
  }

  class Benchmark32 extends AbstractBenchmark {
    override val threads: Int = 32

    @Benchmark
    @OperationsPerInvocation(32)
    override def benchmark() = super.benchmark()
  }

}

object ParallelRedisBenchmarkApp {

  def main(args: Array[String]) {
    val opt: Options = new OptionsBuilder()
      .include(classOf[ParallelRedisBenchmark].getSimpleName)
      .forks(1)
      .warmupIterations(10)
      .jvmArgs("-Xmx4g")
      .build
    new Runner(opt).run
  }

}
