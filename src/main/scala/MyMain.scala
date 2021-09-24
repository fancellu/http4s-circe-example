import cats.Functor
import cats.data.Kleisli

import java.util.concurrent.Executors
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.literal._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server._
import org.http4s.server.middleware._
import org.http4s.server.staticcontent._
import play.twirl.api.Html
import org.http4s.twirl._

import scala.concurrent.duration._
import fs2.Stream
import cats.effect.std.Random

object MyMain extends IOApp {

  case class Greeting(message: String)

  private val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name => Ok(s"Hello, $name.")
  }

  object NameQueryParamMatcher extends QueryParamDecoderMatcher[String]("name")

  private val helloWorldService2 = HttpRoutes.of[IO] {
    case GET -> Root / "hello" :? NameQueryParamMatcher(name) => Ok(s"""Hello, $name.""")
  }

  private val greetService = HttpRoutes.of[IO] {
    case GET -> Root / "greet"  => Ok(Greeting("hello there").asJson)
  }

  private val literal = HttpRoutes.of[IO] {
    case GET -> Root / "literal"  => Ok(
      json"""{ "hello": "buddy" } """)
  }

  val randomDigitIO: IO[Int] = Random.scalaUtilRandom[IO].flatMap(_.nextIntBounded(10))

  private val random = HttpRoutes.of[IO] {
    case GET -> Root / "random"  => Ok {
      randomDigitIO.map(_.toString)
    }
  }

  var myCounter: Int =0

  // hack hack
  private val counter = HttpRoutes.of[IO] {
    case GET -> Root / "counter"  => Ok {
        for {
          _ <- IO.println(s"Counter=$myCounter ")
          _ <- IO{myCounter=myCounter+1}
        } yield myCounter.toString
    }
  }

  val refIntIO: IO[Ref[IO, Int]] =Ref[IO].of(0)

  // better with use of Ref
  private def counter2(counter: Ref[IO, Int]) = HttpRoutes.of[IO] {
    case GET -> Root / "counter2"  => Ok {
        for {
            i <- counter.getAndUpdate(_+1)
            _ <- IO.println(s"Counter=$i")
        } yield i.toString
    }
  }

  private val echoPost = HttpRoutes.of[IO] {
    case req @ POST -> Root / "echo"  => Ok(req.body)
  }

  private val lotsoftext = GZip(HttpRoutes.of[IO] {
    case GET -> Root / "gzip"  => Ok(s"ABCD "*700)
  })

  private val seconds = Stream.awakeEvery[IO](2.second)

  private val mystream = HttpRoutes.of[IO] {
    case GET -> Root / "mystream"  => Ok(seconds.map(dur=> dur.toString))
  }

  private val twirl = HttpRoutes.of[IO] {
    case GET -> Root / "twirl"  => Ok(view.html.index(s"hello from twirl ${new java.util.Date}"))
  }

  private val fs=resourceServiceBuilder[IO]("/").withPathPrefix("/fs").toRoutes

  private val httpApp: IO[Kleisli[IO, Request[IO], Response[IO]]] ={
    for {
      ref <- refIntIO
      rest =(helloWorldService <+> helloWorldService2 <+> greetService <+> literal <+> lotsoftext <+>
        fs <+> mystream <+> twirl <+> echoPost <+> random <+> counter <+> counter2(ref)).orNotFound
    } yield rest
  }

  def run(args: List[String]): IO[ExitCode] = {
    httpApp.flatMap( app=>
     BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success))
  }


}
