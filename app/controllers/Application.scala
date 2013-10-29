package controllers

import play.api._
import play.api.mvc._
import play.api.libs.ws.WS

import com.tridion.util.TCMURI
import com.tridion.ambientdata.AmbientDataContext
import scala.collection.mutable
import scala.concurrent.Future
import java.net.URI
import com.tridion.ambientdata.claimstore.cookie.{ClaimsCookie, ClaimCookieSerializer}


//implicit imports
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConversions._


object Application extends Controller {
  private lazy val baseUrl =  Play.current.configuration.getString("tcdweb").get
  private val pagePattern = "/Pages(PublicationId=%s,ItemId=%s)/PageContent"
  private val cpPattern = "/ComponentPresentations(PublicationId=%s,ComponentId=%s,TemplateId=%s)"

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def page(pageId:String) = {
    val headers = getClaimsAsCookies()

    Action {
      val pageUri = new TCMURI(pageId)
      val url = baseUrl + pagePattern.format(pageUri.getPublicationId, pageUri.getItemId)
      val responseFuture = WS.url(url).withHeaders(headers: _*).get()
      Async {
        responseFuture.map {
          resp => {
            Ok(views.html.page(getContentFromXml(resp.body, "Content")))
          }
        }
      }
    }
  }

  def cp(componentId:String, templateId:String) = {
    val headers = getClaimsAsCookies()

    Action {
      val componentUri = new TCMURI(componentId)
      val templateUri = new TCMURI(templateId)
      val url = baseUrl + cpPattern.format(componentUri.getPublicationId, componentUri.getItemId, templateUri.getItemId)

      val responseFuture = WS.url(url).withHeaders(headers: _*).get()

      Async {
        responseFuture.map(resp => {
            Ok(views.html.cp(getContentFromXml(resp.body, "PresentationContent")))
          }
        )
      }
    }
  }

  def claims = {
    val cs = Option(AmbientDataContext.getCurrentClaimStore)

    Action {
      //this is a future so another thread
      val claimsFuture = Future(cs match {
        case Some(cs) => {
          cs.getAll().map(c => (c._1.toString -> c._2))
        }
        case None => {
          Logger.error("no claim store found")
          mutable.Map[String, AnyRef]()
        }
      })

      Async {
        claimsFuture.map { claims => Ok(views.html.claims(claims)) }
      }
    }
  }

  private def getContentFromXml(xmlText: String, tagName:String):String = {
    val xml = scala.xml.XML.loadString(xmlText)
    val content = xml \\ tagName filter ( z => z.namespace == "http://schemas.microsoft.com/ado/2007/08/dataservices")
    content.text
  }

  private def getClaimsAsCookies() = {
    Option(AmbientDataContext.getCurrentClaimStore) match {
      case Some(cs) => {
        val serializedCookies = serializeClaims(cs.getAll()).map(cookie => {
          ("Cookie", cookie.getName + "=\"" + new String(cookie.getValue) + "\"")
        })

        serializedCookies
      }
      case None => {
        Logger.error("No claimstore, nothing to serialize")
        Nil
      }
    }
  }

  private def serializeClaims(claims:mutable.Map[URI, AnyRef]):java.util.List[ClaimsCookie] = {
     new ClaimCookieSerializer("TAFContext").serializeClaims(claims)
  }
}