package com.gounder.mediaplayer.htd.actor


import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import akka.actor.ActorRef
import akka.actor.Props

import gnu.io.SerialPort
import java.io.InputStream
import gnu.io.CommPortIdentifier
import java.io.OutputStream

/**
 * Generic Serial interactions. 
 *  
 */

object SerialClientActor {
  var serialPort = Option.empty[SerialPort]
  
  def props(replies: ActorRef) =
    Props(classOf[SerialClientActor], replies)
    
    case class SerialCommand( settings : SerialSettings , commands : List[List[ Short ]], debugStr: String)
    
    //Settings and defaults.
    case class SerialSettings( port : String, var baud : Int = 115200, dataBits : Int = 8, stopBits : Int = 1, parity : Int = 0, flowControl : Int = 0, 
      debug : Boolean = false, timeout : Int = 2000, cmdDealy : Int = 100, userName : String = "admin", password : String = "admin", owner : String = "none" )
      
}

class SerialClientActor(listener: ActorRef) extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher
  import SerialClientActor._
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  
   def receive: Receive = {
    case cmd : SerialCommand => {
      
        log.debug( "Serial Command received ... ")
        
        val resp = sendCommands(cmd.settings, cmd.commands, cmd.debugStr )
        listener ! resp
    }
  }

  

  
    
  /**
   * Send commands to serial port described by SerialSettings
   * 
   * @TODO Move the code to an another layer via a device adapter.
   * 
   * @Param settings - SerialSettings
   * @Param cmds - List of commands and annotations
   * 
   */

  def sendCommands ( settings : SerialSettings , commands: List[List[Short]], debugLogStr: String ) = {
    
//        commands.foreach( command => {
//           println( s"$debugLogStr  : " + command.map(_.toHexString.toUpperCase)) 
//         })
           
      
     this.synchronized {
       if(serialPort == null || serialPort.isEmpty ) {
         serialPort = initPort(settings)

       }
         val out = serialPort.get.getOutputStream
         val in = serialPort.get.getInputStream
         println(inputStreamToString(in, 100)) //Clear input buffer

         commands.foreach( command => {
           println( s"$debugLogStr  : " + command.map(_.toHexString.toUpperCase)) 
           command.foreach( x => out.write(x.toByte) )
           out.flush()
           out.write((  "\r").getBytes);
           out.flush()
         
           })


         var rsp = inputStreamToString(in, 1000)
         println ( rsp ) 
        
     }
  }
  
  def initPort (settings : SerialSettings ) : Option[SerialPort] = {
          log.debug( "Serial Connect Settings : "+ settings)
          val portId = CommPortIdentifier.getPortIdentifier(settings.port);
          log.error("Serial Port in use : " +  portId.isCurrentlyOwned()  + " Owner " + portId.getCurrentOwner + " " )
           if (( ! portId.isCurrentlyOwned()) || ! serialPort.isEmpty ){
              var sp = 
                 if( ! portId.isCurrentlyOwned() ) {
                   log.info( "open Serial port ")
                   val commPort = portId.open(settings.owner, settings.timeout)
                   commPort.asInstanceOf[SerialPort]
                 }else if (! serialPort.isEmpty  ){
                   log.info( "Serial port already connected")
                   serialPort.get
                 }else{
                   throw new Exception ( "Unable to open connection")
                 }
              

               log.info( "HTD Serial port Connected")
               
                 
              println( settings )
               sp.setSerialPortParams(settings.baud, settings.dataBits, settings.stopBits, settings.parity)
               sp.setFlowControlMode(settings.flowControl)               

               
               Option(sp)

           }else{
             Option.empty[SerialPort]
           }
      }     
  
        def inputStreamToString(in: InputStream, wait : Int = 5): String= {
         Thread.sleep(wait)
         val v = java.nio.IntBuffer.allocate(8*5)
         val l = in.available()
       if( l > 0 ) {
         val buffer = new Array[Byte](l)
         in.read( buffer, 0, l )
         buffer.foreach( c => print ( c.toChar + " " ) )
         println ("")
         println ( buffer.length )
         new String ( buffer)
       }else{
         ""
       }
        
      }      
  
}