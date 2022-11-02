import MyMain._
import cats.effect.{IO, Ref}
import cats.implicits._
import io.circe.Json
//import cats.syntax._
import org.http4s.{HttpRoutes, Request}
import io.circe.literal._
//import org.http4s._

import org.http4s.circe._


class MainRoutesSuite extends munit.Http4sHttpRoutesSuite {
  override val routes: HttpRoutes[IO] = helloWorldService <+> helloWorldService2 <+> greetService <+> MyMain.literal <+> lotsoftext <+>
          fs <+> echoPost


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
    assertIO(response.as[String].map(_.length), 5*700, clue = "Wrong length")
  }

  test(GET(uri"fs/hello.txt")).alias("Get hello.txt from file system") { response =>
    assertIO(response.as[String], "Hello")
  }

//  test(GET(uri"twirl")).alias("Use twirl") { response =>
//    assertIO(response.as[String].map(_.contains("hello from twirl")), true)
//  }

  test(Request[IO](POST, uri"echo", body = fs2.Stream.emits("woof".getBytes))).alias("posting a body") { response =>
    assertIO(response.as[String], "woof")
  }
}
