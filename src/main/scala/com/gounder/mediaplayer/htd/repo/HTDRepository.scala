package com.gounder.mediaplayer.htd.repo

import java.util.Date
import com.gounder.mediaplayer.htd.rest.DefaultJsonFormats
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.unmarshalling.Unmarshaller
import spray.json.JsValue
import com.gounder.mediaplayer.htd.actor.MediaPlayerActor.HTDCommand
import scala.collection.mutable.ListBuffer
import com.gounder.mediaplayer.htd.actor.MediaPlayerActor.HTDData
import scala.collection.mutable.TreeMap

object HTDRepository extends LazyLogging {
  

  case class HTDZoneConfig(zoneId: String, zoneName: String, volume: String, startVolume: String, input: String, power: Boolean, mute: Boolean, dnd: Boolean )
  
  case class HTDMP3PlayerConfig(mp3PlayerPlay: Boolean, mp3PlayerStartTrack : Int, mp3PlayerRepeatLoop: Boolean)
  case class HDTConfig(  muteAllWhileApply: Boolean, zoneConfigs: List[HTDZoneConfig], mp3PlayerConfg: HTDMP3PlayerConfig, partyMode: Boolean, partyModeInput: String)

  case class UserPermissions( user: String, permissions: List[String]) 
  case class ConfigAuth(ownerUser: String, public: Boolean, permissions: List[UserPermissions])
  
  case class HTDEnvConfig(id: String, name: String, config: HDTConfig, auth: ConfigAuth)
  
  case class ConfigSchedule( scheduleId: String, `type`: String, start: Date, end: Date, configId: String)
  
  case class ConfigRepo(location: String, htdConfigs: List[HTDEnvConfig], schedules: List[ConfigSchedule] )
  
//  def main(args: Array[String]) {
  def getConfig( location: String, configId: String) : HTDEnvConfig = {
    
    
       val zCfg = (1 to 6).toList.map( x => { HTDZoneConfig( "" + x, "D" + x, "20", "20", "12", true,false, false) })
       val mp3Cfg = HTDMP3PlayerConfig(true, 5, false )
       val htdCfg = HDTConfig(true, zCfg, mp3Cfg, false, "12" )
       val up = UserPermissions("all", List("ALL"))
       val cfgAuth = ConfigAuth("venkat", true, List(up))
       val configEnv = HTDEnvConfig("1", "Morning Prayer", htdCfg, cfgAuth )
       
       val cs = ConfigSchedule("1", "HTD", new Date, new Date, "1")
       val cr = ConfigRepo("25broadway",  List(configEnv), List( cs ))
       
       configEnv


  }
  
  def toCommandList(location: String, config: HDTConfig): TreeMap[Int, HTDCommand] = {
    
    var cmds = TreeMap[Int, HTDCommand]()
    var i = 0
    if( config.muteAllWhileApply ) {
      i += 1
      cmds += ( i -> HTDCommand(location, "muteon", "all", None))
    }
      i += 1
      cmds += ( i -> HTDCommand(location, "poweron", "all", None))
    
            i += 1
      cmds += ( i -> HTDCommand(location, "mp3stop", "1", None))

    if( config.partyMode ) {
      i += 1
      cmds += ( i -> HTDCommand(location, "partyinput", "1", Some( HTDData(None, input=Some(config.partyModeInput), None)) ))
    }
    
    i += 1
    cmds += ( i -> HTDCommand(location, "muteoff", "all", None))
    
    
    config.zoneConfigs.foreach(zc => {
      i += 1
      cmds += ( i -> HTDCommand(location, "volume", zc.zoneId, Some(HTDData(volume=Some( zc.volume ), None, None))))    
      i += 1
      cmds += ( i -> HTDCommand(location, "input", zc.zoneId, Some(HTDData(None, input=Some( zc.input ) , None))))          
      
    })

    if( config.mp3PlayerConfg.mp3PlayerPlay ) {
      i += 1
      cmds += ( i -> HTDCommand(location, "mp3play", "1", None))
    }
        
    cmds
      
  }
  
  object HTDFormats extends DefaultJsonFormats {}
  import HTDFormats._
  import spray.json._
  implicit val memberListUnmarshaller: Unmarshaller[JsValue, ConfigRepo] =
        Unmarshaller.strict(jsValue => cfgRepo.read(jsValue))  
        
  def toJson( cr: ConfigRepo) = {
    println(cr.toJson.prettyPrint)
  }
}