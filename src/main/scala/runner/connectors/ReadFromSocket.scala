package runner.connectors

import java.net.{InetAddress, Socket}

import core.Atom
import core.lars.TimeUnit
import runner.{ConnectToEngine, EngineRunner, Int, Startable}

import scala.io.BufferedSource
import scala.util.Try

/**
  * Created by FM on 14.11.16.
  */
case class ReadFromSocket(inputUnit: TimeUnit, port: Int) extends ConnectToEngine {

  private val parser = parseInput(inputUnit) _

  def startWith(engineRunner: EngineRunner): Startable = {
    () => {
      val init = Try(connectToSocket(engineRunner))
      if (init.isFailure) {
        println("Socket connection could not be initialized")
        init.failed.get.printStackTrace()
      }
    }
  }

  private def connectToSocket(engineRunner: EngineRunner) = {

    val socket = new Socket(InetAddress.getByName("localhost"), port)
    lazy val in = new BufferedSource(socket.getInputStream).getLines()


    in.foreach(input => {
      val (time, atoms) = parser(input)

      if (atoms.nonEmpty) {
        engineRunner.append(time.map(engineRunner.convertToTimePoint), atoms)
        println(f"OK,  appending @$time $atoms from socket")
      }
      else
        println(f"could not parse input $input")

    })


  }
}
