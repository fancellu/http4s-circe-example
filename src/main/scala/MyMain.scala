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
import org.http4s.server.blaze._
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

  private val greetService = HttpRoutes.of[IO] {
    case GET -> Root / "greet"  => Ok(Greeting("hello there").asJson)
  }

  private val literal = HttpRoutes.of[IO] {
    case GET -> Root / "literal"  => Ok(json"""{"hello": "buddy"}""")
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

  private val blockingPool = Executors.newFixedThreadPool(4)
  private val blocker = Blocker.liftExecutorService(blockingPool)

  //val fs=fileService[IO](FileService.Config("./src/main/resources", blocker,pathPrefix = "/fs"))
  private val fs=resourceService[IO](ResourceService.Config("/", blocker,pathPrefix = "/fs"))

  private val httpApp=(helloWorldService <+> greetService <+> literal <+> lotsoftext
    <+> fs <+> mystream <+> twirl).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
