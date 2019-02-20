package com.gounder.mediaplayer.htd.rest

import akka.http.scaladsl.server.RouteConcatenation
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.gounder.mediaplayer.htd.actor.MediaPlayerActor
import akka.actor.Props
import akka.http.scaladsl.model.headers.HttpOriginRange.*
import akka.http.scaladsl.model.headers.HttpOrigin
import akka.http.scaladsl.model.HttpMethods._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.gounder.mediaplayer.htd.endpoint.HTDEndpoint
import scala.collection.immutable.Seq
import akka.http.scaladsl.Http

object MediaPlayerService extends App with RouteConcatenation {
  implicit val system = ActorSystem("akka-http-mediaplayer")
  sys.addShutdownHook(system.terminate())
  println( "Start server ...")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val configFile = ConfigFactory.load("broadwayctrl")  
  
  val player = system.actorOf(Props[MediaPlayerActor], name = "mediaplayer")  
  
  val allowedMethods = Seq( GET, POST, HEAD, OPTIONS, PATCH )
  val corssetting = CorsSettings.Default(
            allowGenericHttpRequests = true,
            allowCredentials = true,
            allowedOrigins = *,
            allowedHeaders = HttpHeaderRange.*,
            allowedMethods = allowedMethods,
            exposedHeaders = Seq.empty,
            maxAge = Some(30 * 60)
          )

  val routes = cors(corssetting) ( 
      new HTDEndpoint(player).route
      )
    
  Http().bindAndHandle(routes, "0.0.0.0", configFile.getInt("listenport"))
  
  println( "MediaPlayerService listening on port " + configFile.getInt("listenport"))  
}