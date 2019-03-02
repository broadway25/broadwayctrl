package com.gounder.mediaplayer.htd.actor

import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.ConfigFactory
import akka.util.ByteStringBuilder
import com.gounder.mediaplayer.htd.actor.SerialClientActor.SerialSettings
import com.gounder.mediaplayer.htd.actor.SerialClientActor.SerialCommand
import scala.collection.mutable.ListBuffer
import com.gounder.mediaplayer.htd.repo.HTDRepository

object MediaPlayerActor {
  
  case class HTDCommands (
        
  ) {}
  val zones : Map[String, Short] = Map ( 
           "all" -> 0x00
          ,"1" -> 0x01
          ,"2" -> 0x02
          ,"3" -> 0x03
          ,"4" -> 0x04
          ,"5" -> 0x05
          ,"6" -> 0x06
          
        )
        
  // Setup 12 input channels. Converts 1 index to an input index starting at x10 == 16.
  val inputIds: Map[String, Short]  = ( 0 to 11 ).toList.map(v => ((v +1) + "", (16 + v).toShort ) ).toMap
 
  // Setup 12 input channels. Converts 1 index to an input index starting at x36 == 54.
  val partyInputIds: Map[String, Short]  = ( 0 to 11 ).toList.map(v => ((v +1) + "", (54 + v).toShort ) ).toMap
  
  
  //Volume map starts at C4 for Zero. FF for 59 and 00 for 60 
  val volumeNos: Map[String, Short]  = ( 1 to 59 ).toList.map(v => (v.toString, (196 + v).toShort ) ).toMap + ("60" -> 0x00)
  
    
  val cmdCodePowerAndVol: Short = 0x04
  val cmdCodeVol: Short = 0x15
  val cmdCodeVolRepeat: Short = 0x00
  
  //Power ON  => All - 55 ; Zone -> 56
  //Power OFF => All - 57 ; Zone -> 58
  val htdCommands : Map[String, Short] = Map(
          "poweron" ->  0x55 
          , "poweroff" -> 0x56
          , "muteon" -> 0x1E
          , "muteoff" -> 0x1F
          , "dndon" -> 0x59
          , "dndoff" -> 0x5A
          , "mp3repeaton" -> 0xFF
          , "mp3repeatoff" -> 0x00
          , "mp3forward" -> 0x0A
          , "mp3play" -> 0x0B
          , "mp3back" -> 0x0C
          , "mp3stop" -> 0x0D
          , "volume" -> 0x31
          , "input" -> 0x32
          , "partyinput" -> 0x33
        )
  
        
  case class HTDCommand(location: String, command: String, zone: String , data: Option[HTDData] )

  case class HTDData(volume: Option[String], input: Option[String], partyModeSource: Option[String])
  
  case class HTDCommandResponse(status: String)
  
  case class HTDAutoCommand( location: String, configId: String )
  
}



class MediaPlayerActor extends Actor with ActorLogging {
  import MediaPlayerActor._
  
      val serialActor = context.system.actorOf(SerialClientActor.props(self));
      val configFile = ConfigFactory.load("broadwayctrl")
         

  def receive(): Receive = {


    case cmd: HTDCommand => {
      val (resp, cmdDataList) = htdCommandToCmdDataList( cmd )
      
      sender ! resp
      
       if ( ! cmdDataList.isEmpty && cmdDataList.get.size > 0 ) {    
            val ss = SerialSettings(configFile.getString("media.audio." + cmd.location + ".htd.serial.port" ),
            configFile.getInt("media.audio." + cmd.location + ".htd.serial.baud" ))
            serialActor ! SerialCommand( ss, cmdDataList.get, cmd.toString  )
       }
      
    }
    
    case HTDAutoCommand(location, configId) => {
      
        val ss = SerialSettings(configFile.getString("media.audio." + location + ".htd.serial.port" ),
        configFile.getInt("media.audio." + location + ".htd.serial.baud" ))
        
        val config = HTDRepository.getConfig(location, configId)
          
        val cmds = HTDRepository.toCommandList(location, config.config)
        
        val cmdStrs = cmds.map(v => (v._1 -> ( s"$v._1 => $v._2.toString", htdCommandToCmdDataList( v._2 )._2.get)))
       
        cmdStrs.foreach(cs => {
          serialActor ! SerialCommand( ss, cs._2._2, cs._2._1  )
        })

        sender ! HTDCommandResponse( s"Loading Config $configId" )
    }
    
  }
      
      
      
      
      
  def htdCommandToCmdDataList (htdCmd: HTDCommand ): (HTDCommandResponse, Option[List[List[Short]]]) = {
    
      val command = htdCmd.command
      val location = htdCmd.location
      val zoneId = htdCmd.zone
      val data = htdCmd.data

      //Format \x02\x00\x01\x04\x57\x5E
      // 0x02 and 0x00 - Constant
      // 0x01 Zone identifier - 0x00 all.
      // 0x04 - Command Code
      // 0x57 - Data
      // ox5E - Checksum      
      log.debug(location, command, zoneId, data )
      
      if ( zones.get(zoneId).isEmpty || htdCommands.get(command).isEmpty ) {
          
          log.debug( "Invlid Zone or command")
          (HTDCommandResponse( "Invlid Zone or command"), None)
      } else if ( command == "volume" && ( data.isEmpty || data.get.volume.isEmpty || volumeNos.get(data.get.volume.get).isEmpty )) {
          log.debug( "Invlid Volume")
          (HTDCommandResponse( "Invlid Volume"), None )
          
      } else if ( command == "input" && ( data.isEmpty || data.get.input.isEmpty || inputIds.get(data.get.input.get).isEmpty )) {
          log.debug( inputIds.toString );
          log.debug( "Invlid Input")
          (HTDCommandResponse( "Invlid Input"), None)
          
      } else if ( command == "partyinput" && ( data.isEmpty || data.get.partyModeSource.isEmpty || partyInputIds.get(data.get.partyModeSource.get).isEmpty )) {
          log.debug( "Invlid Party Input")
          (HTDCommandResponse( "Invlid Party Input"), None )
          
      } else {
        log.debug( "Valid Command")

            
        var zone = zones.get(zoneId).get
        if( command.startsWith("mp3") ) {
          zone = 0x00
        }
       
        val dataVal = htdCommands.get(command).get
                                // Zero all zones , _ve no zone id
        val cmdCodeData: (Short, Short, Short) = (dataVal, zone ) match {
                                      //Power command. 
                                      // If Zone is all(0) Data power on 55, off 56
                                      // If single zone - Data : Power on 57, power off 58 - (z compare 0) + 2 - returns 0 for all and 2 for others. 
                                      case (0x55 | 0x56,  z ) => (0x00, 0x04, (dataVal + ( if ( zone > 0 ) 2 else 0) ).toShort) ;
                                      // MP3 Repeat command code 01
                                      case (0xFf | 0x00,  z ) => (0x00, 0x01, dataVal) ;
                                      //Volume - Command Code 15 , Data passed in 
                                      // Volume second byte is 01  others 00
                                      case (0x31,  z ) => (0x01, 0x15, volumeNos.get(data.get.volume.get).getOrElse(0x00)) ;
                                      //Input 
                                      case (0x32,  z ) => (0x00, 0x04, inputIds.get(data.get.input.get).getOrElse(0x00)) ;
                                      //Party Input
                                      case (0x33,  z ) => (0x00, 0x04, partyInputIds.get(data.get.partyModeSource.get).getOrElse(0x00)) ;
                                      case _ => (0x00, 0x04, dataVal) 
                                     }
         
        val zoneIds = new ListBuffer[Short]()
        
        //If Zone is all walk through all zones and create commands
        if( ! command.startsWith("power" ) && zoneId == "all" && ! command.startsWith("partyinput" )) {
          zones.filterKeys(_ != "all").foreach(z => zoneIds += z._2 )
        } else if( command.startsWith("partyinput" ) ) {
          zoneIds += data.get.partyModeSource.get.toShort      
        } else {
          zoneIds += zone
        }
    
        
        val cmdDataList = zoneIds.sorted.map( zid => {
                //Format \x02\x00\x01\x04\x57\x5E
                // 0x02, 0x00, Zone (00 ALL or ID  starting 1), 04 0r 15 - Vol, Data , Check Sum
                val cmdList : List[Short] = List(0x02, cmdCodeData._1, zid , cmdCodeData._2, cmdCodeData._3 ) ;
                val checkSum = (cmdList.foldLeft(0) ( (_ + _) )).toShort
                //Calculate checksum. If checksum over 255(FF) round dwon by FF
                val cmdListChkSum = cmdList :+ {if ( checkSum > 255 ) {checkSum - 256} else {checkSum }}.toShort
//                log.debug ("Command List : " +  cmdListChkSum.map(_.toHexString.toUpperCase) );
                cmdListChkSum
              }
            ).toList


         (HTDCommandResponse( "Processed"), Some(cmdDataList))            
        
      }
        
    
  }
      
}