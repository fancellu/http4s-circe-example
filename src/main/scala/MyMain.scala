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

  val httpApp=(helloWorldService <+> greetService <+> literal).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
