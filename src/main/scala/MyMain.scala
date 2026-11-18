import cats.effect._
import cats.effect.std.Random
import cats.implicits._
import com.comcast.ip4s._
import fs2.Stream
import io.circe.generic.auto._
import io.circe.literal._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.server.middleware._
import org.http4s.server.staticcontent._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

object MyMain extends IOApp.Simple {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  val Host: Host = host"localhost"
  val Port: Port = port"8080"
  val selfUri: Uri = Uri.unsafeFromString(s"http://$Host:$Port")

  case class Greeting(message: String)

  val helloWorldService = HttpRoutes.of[IO] {
    case req @ GET -> Root / "hello" / name =>
      val remoteAddr = req.remoteAddr.map(ip => s"You are on $ip").getOrElse("")
      Logger[IO].info("/hello endpoint") *> Ok(s"Hello, $name. $remoteAddr")
  }

  object NameQueryParamMatcher extends QueryParamDecoderMatcher[String]("name")

  val helloWorldService2 = HttpRoutes.of[IO] {
    case GET -> Root / "hello" :? NameQueryParamMatcher(name) => Ok(s"""Hello, $name.""")
  }

  val greetService = HttpRoutes.of[IO] {
    case GET -> Root / "greet" => Logger[IO].info("/greet endpoint") *> Ok(Greeting("hello there").asJson)
  }

  val literal = HttpRoutes.of[IO] {
    case GET -> Root / "literal" => Ok(json"""{ "hello": "buddy" } """)
  }

  val randomDigitIO: IO[Int] = Random.scalaUtilRandom[IO].flatMap(_.nextIntBounded(10))

  val random = HttpRoutes.of[IO] {
    case GET -> Root / "random" => Ok {
      for {
        int <- randomDigitIO
        _ <- Logger[IO].info(s"Random int is $int")
      } yield int.toString
    }
  }

  var myCounter: Int = 0

  // Deliberately hacky: shared mutable state outside of any effect type.
  // Not safe under concurrent requests, see counter2 for the proper approach.
  val counter = HttpRoutes.of[IO] {
    case GET -> Root / "counter" => Ok {
      for {
        _ <- Logger[IO].info(s"Counter=$myCounter")
        _ <- IO {
          myCounter = myCounter + 1
        }
      } yield myCounter.toString
    }
  }

  val refIntIO: IO[Ref[IO, Int]] = Ref[IO].of(0)

  // Better, with use of Ref (atomic, purely functional)
  def counter2(counter: Ref[IO, Int]) = HttpRoutes.of[IO] {
    case GET -> Root / "counter2" => Ok {
      for {
        i <- counter.getAndUpdate(_ + 1)
        _ <- Logger[IO].info(s"Counter2=$i")
      } yield i.toString
    }
  }

  val echoPost = HttpRoutes.of[IO] {
    case req @ POST -> Root / "echo" => Ok(req.body)
  }

  val lotsoftext = GZip(HttpRoutes.of[IO] {
    case GET -> Root / "gzip" => Ok("ABCD " * 700)
  })

  val seconds = Stream.awakeEvery[IO](2.second)

  val mystream = HttpRoutes.of[IO] {
    case GET -> Root / "mystream" => Ok(seconds.map(dur => dur.toString + "\n"))
  }

  val slow = HttpRoutes.of[IO] {
    case GET -> Root / "slow" =>
      Logger[IO].info("Sleeping") *> IO.sleep(4.seconds) *> Logger[IO].info("Awake") *>
        Ok("I am slow, because I have been sleeping")
  }

  val fs = resourceServiceBuilder[IO]("/").withPathPrefix("/fs").toRoutes

  val handleForm = HttpRoutes.of[IO] {
    case req @ POST -> Root / "handle_form" =>
      req.decode[UrlForm] { urlForm =>
        val userName = urlForm.getFirstOrElse("username", "")
        Logger[IO].info(s"Form username=$userName") *> Ok(s"Hello $userName")
      }
  }

  // ---------- Client examples ----------

  case class Post(userId: Int, id: Int, title: String, body: String)

  val HOST = uri"https://jsonplaceholder.typicode.com"

  // If we choose to hand derive coder/decoder
  //  implicit val postDecoder = jsonOf[IO, Post]
  //  implicit val postEncoder = jsonEncoderOf[IO, Post]

  def clientRoutes(client: Client[IO]) = HttpRoutes.of[IO] {
    // returning the json as a string
    case GET -> Root / "client" / "todos" =>
      client.expect[String](HOST / "todos").flatMap(str => Ok(str))

    case GET -> Root / "client" / "todos" / IntVar(id) =>
      // we handle 404s, else seen as 500
      client.expect[String](HOST / "todos" / id).flatMap(str => Ok(str)).recoverWith {
        case UnexpectedStatus(NotFound, _, _) => NotFound()
      }

    // returning the json as a string
    case GET -> Root / "client" / "users" =>
      client.expect[String](HOST / "users").flatMap(str => Ok(str))

    case GET -> Root / "client" / "users" / IntVar(id) =>
      // we return a string with 200 on error
      client
        .expect[String](HOST / "users" / id)
        .handleError(th => s"Something went wrong $th")
        .flatMap(str => Ok(str))

    // returning as a case class, using codec to derive without having to do it myself
    case GET -> Root / "client" / "posts" =>
      import org.http4s.circe.CirceEntityCodec._
      client.expect[List[Post]](HOST / "posts").flatMap(posts => Ok(posts))

    case GET -> Root / "client" / "posts" / IntVar(id) =>
      import org.http4s.circe.CirceEntityCodec._
      // on error (of any type) we simply return some dummy post
      client
        .expect[Post](HOST / "posts" / id)
        .onError(th => Logger[IO].error(s"***Error*** $th"))
        .orElse(IO.pure(Post(id, 1000, "", "")))
        .flatMap(post => Ok(post))

    // calling ourself on the /slow endpoint, timing out and falling back to a canned value
    case GET -> Root / "client" / "slow" =>
      client
        .expect[String](selfUri / "slow")
        .timeoutTo(1.seconds, IO.pure("Timedout so falling back to canned value"))
        .flatMap(str => Ok(str))

    // calling ourself on the /slow endpoint, twice, in parallel
    case GET -> Root / "client" / "twiceslow" =>
      val url = selfUri / "slow"
      for {
        _ <- Logger[IO].info("/twiceslow endpoint")
        tuple <- (client.expect[String](url), client.expect[String](url)).parTupled
        _ <- Logger[IO].info("/twiceslow ended")
        resp <- Ok(tuple._1 + "\n" + tuple._2)
      } yield resp
  }

  // ---------- OpenAI image generation ----------

  case class ImageGenRequest(prompt: String, model: String = "gpt-image-1-mini", n: Int = 1, size: String = "1024x1024")
  case class ImageData(b64_json: Option[String], url: Option[String])
  case class ImagesResponse(created: Long, data: List[ImageData])

  object PromptQueryParamMatcher extends QueryParamDecoderMatcher[String]("prompt")

  def openAIRoutes(client: Client[IO]) = HttpRoutes.of[IO] {
    case GET -> Root / "openai" / "image" :? PromptQueryParamMatcher(prompt) =>
      import _root_.scalatags.Text.all._
      import org.http4s.circe.CirceEntityCodec._
      import org.http4s.client.dsl.io._
      import org.http4s.scalatags._

      def page(content: Frag) = html(body(h3(prompt), content))

      sys.env.get("OPENAI_API_KEY").filter(_.nonEmpty) match {
        case None =>
          BadRequest("OPENAI_API_KEY environment variable is not set")
        case Some(openAIKey) =>
          val request = POST(
            ImageGenRequest(prompt = prompt).asJson,
            uri"https://api.openai.com/v1/images/generations",
            Authorization(Credentials.Token(AuthScheme.Bearer, openAIKey))
          )

          // expectOr lets us surface OpenAI's error body (bad key, quota, moderation...) rather than just the status
          val call = client.expectOr[ImagesResponse](request) { resp =>
            resp.as[String].map(body => new RuntimeException(s"${resp.status}: $body"))
          }

          call.attempt.flatMap {
            case Right(resp) =>
              resp.data.headOption.flatMap(d => d.b64_json.map(b64 => s"data:image/png;base64,$b64").orElse(d.url)) match {
                case Some(src0) => Ok(page(img(src := src0)))
                case None       => Ok(page(h1("No image returned")))
              }
            case Left(th) =>
              Logger[IO].error(s"OpenAI call failed: $th") *> Ok(page(div(h1("Image generation failed"), pre(th.toString))))
          }
      }
  }

  // ---------- Wiring ----------

  def httpApp(client: Client[IO]): IO[HttpApp[IO]] =
    refIntIO.map { ref =>
      (helloWorldService <+> helloWorldService2 <+> greetService <+> literal <+> lotsoftext <+>
        fs <+> mystream <+> echoPost <+> random <+> counter <+> counter2(ref) <+> clientRoutes(client) <+>
        openAIRoutes(client) <+> slow <+> handleForm).orNotFound
    }

  val run: IO[Unit] =
    EmberClientBuilder.default[IO].build.use { client =>
      for {
        app <- httpApp(client)
        _ <- Logger[IO].info(s"Starting server on $selfUri")
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(Host)
          .withPort(Port)
          .withHttpApp(app)
          .build
          .useForever
      } yield ()
    }
}
