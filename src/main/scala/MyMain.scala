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
import io.circe.generic.auto._

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

 // private val blockingPool = Executors.newFixedThreadPool(4)

  private val fs=resourceServiceBuilder[IO]("/").withPathPrefix("/fs").toRoutes

  private val httpApp=(helloWorldService <+> helloWorldService2 <+> greetService <+> literal <+> lotsoftext
    <+> fs <+> mystream <+> twirl <+> echoPost).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  // add doobie/quill endpoints
  // H2 or postgres?
  // unit test
  // flywaydb

}
