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
import org.http4s.server.blaze._

import org.http4s.server.middleware._

object MyMain extends IOApp {

  import io.circe.generic.auto._

  case class Greeting(message: String)

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name => Ok(s"Hello, $name.")
  }

  val greetService = HttpRoutes.of[IO] {
    case GET -> Root / "greet"  => Ok(Greeting("hello there").asJson)
  }

  val literal = HttpRoutes.of[IO] {
    case GET -> Root / "literal"  => Ok(json"""{"hello": "buddy"}""")
  }

  val lotsoftext = GZip(HttpRoutes.of[IO] {
    case GET -> Root / "gzip"  => Ok(s"ABCD ABCD ABCD ABCD ABCD ABCD ABCD "*100)
  })

  val httpApp=(helloWorldService <+> greetService <+> literal <+> lotsoftext).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

// put in https://github.com/http4s/rho
}
