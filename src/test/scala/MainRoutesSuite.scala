import MyMain._
import cats.effect.{IO, Ref}
import cats.implicits._
import io.circe.Json
import io.circe.literal._
import org.http4s._
import org.http4s.circe._

class MainRoutesSuite extends munit.Http4sSuite {

  private val routes: HttpRoutes[IO] =
    helloWorldService <+> helloWorldService2 <+> greetService <+> MyMain.literal <+> lotsoftext <+>
      fs <+> echoPost <+> random

  override def http4sMUnitClientResource = routes.orFail.asClient

  test(GET(uri"hello" / "Dino")).alias("Say hello to Dino") { response =>
    assertIO(response.as[String], "Hello, Dino. ")
  }

  test(GET(uri"hello?name=Dino")).alias("Say hello to Dino again") { response =>
    assertIO(response.as[String], "Hello, Dino.")
  }

  test(GET(uri"greet")).alias("Greet") { response =>
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
    import io.circe.generic.auto._
    assertIO(response.as[Greeting], Greeting("hello there"))
  }

  test(GET(uri"literal")).alias("Literal") { response =>
    assertIO(response.as[Json], json"""{ "hello": "buddy" }""")
  }

  test(GET(uri"gzip")).alias("Lots of text") { response =>
    assertIO(response.as[String].map(_.length), 5 * 700, clue = "Wrong length")
  }

  test(GET(uri"fs/hello.txt")).alias("Get hello.txt from classpath") { response =>
    assertIO(response.as[String], "Hello")
  }

  test(Request[IO](POST, uri"echo").withEntity("woof")).alias("Posting a body echoes it back") { response =>
    assertIO(response.as[String], "woof")
  }

  test(GET(uri"random")).alias("Random digit") { response =>
    assertIO(response.as[String].map(_.matches("[0-9]")), true)
  }

  test("counter2 increments using Ref") {
    for {
      ref <- Ref[IO].of(0)
      app = counter2(ref).orNotFound
      first <- app.run(Request[IO](GET, uri"/counter2")).flatMap(_.as[String])
      second <- app.run(Request[IO](GET, uri"/counter2")).flatMap(_.as[String])
    } yield assertEquals((first, second), ("0", "1"))
  }

  test("handle_form greets the posted username") {
    val req = Request[IO](POST, uri"/handle_form").withEntity(UrlForm("username" -> "Dino"))
    assertIO(handleForm.orNotFound.run(req).flatMap(_.as[String]), "Hello Dino")
  }
}
