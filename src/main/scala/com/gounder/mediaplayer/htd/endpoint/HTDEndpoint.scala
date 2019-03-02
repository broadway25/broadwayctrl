package com.gounder.mediaplayer.htd.endpoint

import com.gounder.mediaplayer.htd.rest.DefaultJsonFormats
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes
import com.gounder.mediaplayer.htd.actor.MediaPlayerActor._
import com.gounder.mediaplayer.htd.actor.MediaPlayerActor.HTDData



class HTDEndpoint(actor: ActorRef)(implicit executionContext: ExecutionContext) extends Directives with DefaultJsonFormats with LazyLogging {
      val log = logger
      implicit val timeout = Timeout(2.seconds)
//      import HTDEndpoint._

     val route = zoneCtrl ~ configPlayCtrl
      
     def zoneCtrl =
      path("media" / "audio"/ Segment  / Segment / Segment) { (location, zoneId, action) =>
        put {
          extractRequest { request =>

              logUser( request, "PUT", s"media/audio/$location/$zoneId/$action")
              
              import spray.json._
              entity(as[HTDData]) { (data) => 
                logger.debug( data.toString );
                complete { ( actor ? HTDCommand(location, action, zoneId, Some(data))).mapTo[HTDCommandResponse] }  
              }

          }
      }
    } 

  def configPlayCtrl =
      path("media" /  "configplay" / Segment  / Segment ) { (location, configId) =>
        put {
          extractRequest { request =>

              logUser( request, "GET", s"media/audio/$location/$configId")
              
              import spray.json._
              complete { ( actor ? HTDAutoCommand(location, configId)).mapTo[HTDCommandResponse] }  

          }
      }
    } 

//   def inputctrl =
//      path("zone"  / "inut" / Segment / Segment) { (zoneId, inputId) =>
//        get {
//          extractRequest { request =>
//
//              logUser( request, "GET", s"zone/$zoneId/$inputId")
//              
//              import spray.json._
//
//              actor ! SetInput( zoneId, inputId )
//              
//              complete { StatusCodes.OK }
//          }
//      }
//    } 
    def logUser( request: HttpRequest, method: String, action: String ) {
          val header = request.headers.seq.filter( p => p.name == "user")
          if( ! header.isEmpty ) {
            log.debug( method + " " + action + " => " + header.head  )
          }else {
            log.debug( method + " " + action + " => no user" )
          }
    }
    
      
      
}