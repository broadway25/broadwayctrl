package com.gounder.mediaplayer.htd.rest


import java.util.UUID
import scala.reflect.ClassTag
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.gounder.mediaplayer.htd.repo.HTDRepository._
import com.gounder.mediaplayer.htd.actor.MediaPlayerActor._


final case class PrettyPrintedItem(`type`: String, value: String)


trait DefaultJsonFormats extends DefaultJsonProtocol with SprayJsonSupport {
  import DateMarshalling._
  
  implicit val htdData = jsonFormat3 ( HTDData )
  implicit val htdResp = jsonFormat1( HTDCommandResponse )

  implicit val htdzoneCfg = jsonFormat8( HTDZoneConfig )
  implicit val mp3PlayerCfg = jsonFormat3( HTDMP3PlayerConfig )
  implicit val htdCfg = jsonFormat5( HDTConfig )
  implicit val userPerm = jsonFormat2( UserPermissions )
  implicit val cfgAuth = jsonFormat3( ConfigAuth )
  implicit val htdEnvCfg = jsonFormat4( HTDEnvConfig )
  implicit val cfgSch = jsonFormat5( ConfigSchedule )
  implicit val cfgRepo = jsonFormat3( ConfigRepo )
  
  
  /**
   * Computes ``RootJsonFormat`` for type ``A`` if ``A`` is object
   */
  def jsonObjectFormat[A : ClassTag]: RootJsonFormat[A] = new RootJsonFormat[A] {
    val ct = implicitly[ClassTag[A]]
    def write(obj: A): JsValue = JsObject("value" -> JsString(ct.runtimeClass.getSimpleName))
    def read(json: JsValue): A = ct.runtimeClass.newInstance().asInstanceOf[A]
  }

  /**
   * Instance of the ``RootJsonFormat`` for the ``j.u.UUID``
   */
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID) = JsString(x.toString)
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x           => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
    
}