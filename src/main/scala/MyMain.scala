import cats.effect._
import cats.implicits._
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server.Router

import org.http4s._
import org.http4s.dsl.io._
import scala.concurrent.ExecutionContext.Implicits.global

object MyMain extends IOApp {

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }.orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(helloWorldService)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
