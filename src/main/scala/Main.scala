import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes}
import akka.stream.{ActorMaterializer, IOResult}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import scalatags.Text.all._
import scalatags.Text

import scala.concurrent.Future
import scala.io.StdIn

object Main extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val route = {
    concat(
      pathSingleSlash {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HTML.index))
      },
      get {
        path("cv") {
          complete(HttpHelper.pdf(HTML.cv))
        }
      },
      path("contact") {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HTML.contact))
      },
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, HTML.notFound))
    )
  }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8989)

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}

object HttpHelper {
  def texthtml(content: String): HttpEntity = HttpEntity(ContentTypes.`text/html(UTF-8)`, content)
  def pdf(fileStream: Source[ByteString, Future[IOResult]]) = HttpEntity(MediaTypes.`application/pdf`, fileStream)
}

object HTML {

  def cv = FileIO.fromPath(Paths.get("cv.pdf"))

  def notFound: String = {
    val routeList = List(
      "GET /",
      "GET /cv",
      "GET /contact"
    )
    html(
      body(
        h1("404 :("),
        p("These endpoints are available:"),
        ul(routeList.map(li(_)))
      )
    ).render
  }

  def header: List[Text.TypedTag[String]] = List(
    h1("Niels Schrøder Pedersen"),
    p("Cand.it"),
    hr,
    menu,
    hr,
  )

  def menu: Text.TypedTag[String] = div(display.inline)(
    p(display.inline)("menu:  "),
    a(href:="/contact")("contact"),
    p(display.inline)(" | "),
    a(href:="/cv")("CV"),
  )

  def contact: String = {
    val github_link = "https://github.com/nielspedersen/"
    val linkedin_link = "https://www.linkedin.com/in/nielsschroderpedersen/"
    html(
      body(
        header,
        div(display.inline)(
          p("Email: niels at nscp.dk"),
          p("Github: ")(a(href:=github_link, target:="_blank")(github_link)),
          p("LinkedIn: ")(a(href:=linkedin_link, target :="_blank")(linkedin_link)),
        )
      )
    )
  }.render

  def index: String = {
    html(
      body(
        header,
        p(
          """
            |This is my website. It's written in Scala using ScalaTags and Akka Http, and hosted on DigitalOcean
            |using nginx. I'm not very good with front-end, so the site is not so beautiful or complex. It just conveys information :)
            |""".stripMargin
        ),
        p(
          """
            | I'm currently working as a backend developer/data engineer/dev ops at a small startup called Nøie.
            | We develop creams for your skin, and we use statistics to make the creams better. I don't
            | have much to do with creams, but I make sure that all the data programs are running on our AWS account.
            |""".stripMargin
        ),
        p("Here is an unexhaustive list of what I do in my daily work life:"),
        ul(
          li("Cyberinfrastructure on AWS using CloudFormation"),
          li("Data pipelines"),
          li("API endpoints"),
        ),
        p("I work with Python, Scala, Docker, PostgreSQL, bash, Linux, and AWS among other things."),
      )
    )
  }.render
}

