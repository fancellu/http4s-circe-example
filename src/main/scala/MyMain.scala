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

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MyMain extends IOApp {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  case class Greeting(message: String)

  private val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name => Logger[IO].info("/hello endpoint") *> Ok(s"Hello, $name.")
  }

  object NameQueryParamMatcher extends QueryParamDecoderMatcher[String]("name")

  private val helloWorldService2 = HttpRoutes.of[IO] {
    case GET -> Root / "hello" :? NameQueryParamMatcher(name) => Ok(s"""Hello, $name.""")
  }

  private val greetService = HttpRoutes.of[IO] {
    case GET -> Root / "greet" => Logger[IO].info("/greet endpoint") *> Ok(Greeting("hello there").asJson)
  }

  private val literal = HttpRoutes.of[IO] {
    case GET -> Root / "literal" => Ok(
      json"""{ "hello": "buddy" } """)
  }

  val randomDigitIO: IO[Int] = Random.scalaUtilRandom[IO].flatMap(_.nextIntBounded(10))

  private val random = HttpRoutes.of[IO] {
    case GET -> Root / "random" => Ok {
      for {
        int <- randomDigitIO
        _ <- Logger[IO].info(s"Random int is $int")
      } yield int.toString
    }
  }

  var myCounter: Int = 0

  // hack hack
  private val counter = HttpRoutes.of[IO] {
    case GET -> Root / "counter" => Ok {
      for {
        _ <- Logger[IO].info(s"Counter=$myCounter")
        _ <- IO.println(s"Counter=$myCounter ")
        _ <- IO {
          myCounter = myCounter + 1
        }
      } yield myCounter.toString
    }
  }

  val refIntIO: IO[Ref[IO, Int]] = Ref[IO].of(0)

  // better with use of Ref
  private def counter2(counter: Ref[IO, Int]) = HttpRoutes.of[IO] {
    case GET -> Root / "counter2" => Ok {
      for {
        i <- counter.getAndUpdate(_ + 1)
        _ <- IO.println(s"Counter=$i")
      } yield i.toString
    }
  }

  private val echoPost = HttpRoutes.of[IO] {
    case req@POST -> Root / "echo" => Ok(req.body)
  }

  private val lotsoftext = GZip(HttpRoutes.of[IO] {
    case GET -> Root / "gzip" => Ok(s"ABCD " * 700)
  })

  private val seconds = Stream.awakeEvery[IO](2.second)

  private val mystream = HttpRoutes.of[IO] {
    case GET -> Root / "mystream" => Ok(seconds.map(dur => dur.toString))
  }

  private val twirl = HttpRoutes.of[IO] {
    case GET -> Root / "twirl" => Ok(view.html.index(s"hello from twirl ${new java.util.Date}"))
  }

  import org.http4s.blaze.client._
  import org.http4s.client._
  import scala.concurrent.ExecutionContext.global

  case class Post(userId: Int, id: Int, title: String, body: String)

  val HOST = "https://jsonplaceholder.typicode.com"

  // If we choose to hand derive coder/decoder

  //  implicit val postDecoder = jsonOf[IO, Post]
  //  implicit val postEncoder= jsonEncoderOf[IO, Post]

  //  implicit val postsDecoder = jsonOf[IO, List[Post]]
  //  implicit val postsEncoder = jsonEncoderOf[IO, List[Post]]

  private val clientRoute = HttpRoutes.of[IO] {
    // returning the json as a string
    case GET -> Root / "client" / "todos" =>
      val out: IO[String] = BlazeClientBuilder[IO].resource.use { client =>
        val url = s"$HOST/todos"
        client.expect[String](url)
      }
      out.flatMap(str => Ok(str))
    case GET -> Root / "client" / "todos" / IntVar(id) =>
      val stringIO: IO[String] = BlazeClientBuilder[IO].resource.use { client =>
        val url = s"$HOST/todos/$id"
        client.expect[String](url)
      }
      // we handle 404s, else seen as 500

      stringIO.flatMap(str => Ok(str)).recoverWith {
        case UnexpectedStatus(NotFound, _, _) => NotFound()
      }

    // returning the json as a string
    case GET -> Root / "client" / "users" =>
      val out: IO[String] = BlazeClientBuilder[IO].resource.use { client =>
        val url = s"$HOST/users"
        client.expect[String](url)
      }
      out.flatMap(str => Ok(str))
    case GET -> Root / "client" / "users" / IntVar(id) =>
      val stringIO: IO[String] = BlazeClientBuilder[IO].resource.use { client =>
        val url = s"$HOST/users/$id"
        client.expect[String](url)
      }
      // we return a string with 200 on error
      stringIO.handleError(th => s"Something went wrong $th").flatMap(str => Ok(str))
    // returning as a case class
    case GET -> Root / "client" / "posts" =>
      // using codec to derive without having to do it myself
      import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
      import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

      val postsIO = BlazeClientBuilder[IO].resource.use { client =>
        val url = s"$HOST/posts"
        client.expect[List[Post]](url)
      }
      postsIO.flatMap(posts => Ok(posts))
    case GET -> Root / "client" / "posts" / IntVar(id) =>
      import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
      import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
      val postIO = BlazeClientBuilder[IO].resource.use { client =>
        val url = s"$HOST/posts/$id"
        client.expect[Post](url)
      }

      // on error (of any type) we simply return some dummy post
      postIO.orElse {
        IO(Post(id, 1000, "", ""))
      }.flatMap(post => Ok(post))

    //      postIO.onError{th=>
    //        Logger[IO].error(s" Error $th") *> IO.unit}.flatMap(post => Ok(post))
  }


  private val fs = resourceServiceBuilder[IO]("/").withPathPrefix("/fs").toRoutes

  private val httpApp: IO[Kleisli[IO, Request[IO], Response[IO]]] = {
    for {
      ref <- refIntIO
      rest = (helloWorldService <+> helloWorldService2 <+> greetService <+> literal <+> lotsoftext <+>
        fs <+> mystream <+> twirl <+> echoPost <+> random <+> counter <+> counter2(ref) <+> clientRoute).orNotFound
    } yield rest
  }

  def run(args: List[String]): IO[ExitCode] = {

    for {
      app <- httpApp
      _ <- IO.println("logging with a simple IO.println")
      _ <- Logger[IO].info("Logging with Logger[F]")
      exitCode <- BlazeServerBuilder[IO]
        .bindHttp(8080, "localhost")
        .withHttpApp(app)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode
  }


}
