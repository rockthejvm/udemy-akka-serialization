package part2_serialization

import akka.actor.{Actor, ActorLogging}

class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m =>
      log.info(s"Received: $m from ${sender()}")
  }
}
