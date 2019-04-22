package part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.collection.immutable

class SimpleActor extends Actor with ActorLogging with Stash {
  override def receive: Receive = {
    case "createChild" =>
      val childActor = context.actorOf(Props[SimpleActor], "myChild")
      childActor ! "hello"
    case "stashThis" =>
      stash()
    case "change handler NOW" =>
      unstashAll()
      context.become(anotherHandler)

    case "change" => context.become(anotherHandler)
    case message => println(s"I received: $message")
  }

  def anotherHandler: Receive = {
    case message => println(s"In another receive handler: $message")
  }

  override def preStart(): Unit = {
    log.info("I'm starting")
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: RuntimeException => Restart
    case _ => Stop
  }
}

class SimplePersistentActor extends PersistentActor with ActorLogging {
  override def persistenceId: String = "simple-persistent-actor"

  override def receiveCommand: Receive = {
    case m => persist(m) { _ =>
      log.info(s"I have persisted: $m")
    }
  }

  override def receiveRecover: Receive = {
    case event =>
      log.info(s"Recovered: $event")
  }
}

object AkkaRecap extends App {

  // actor encapsulation
  val system = ActorSystem("AkkaRecap")
  // #1: you can only instantiate an actor through the actor system
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")
  // #2: sending messages
  actor ! "hello"
  /*
    - messages are sent asynchronously
    - many actors (in the millions) can share a few dozen threads
    - each message is processed/handled ATOMICALLY
    - no need for locks
   */

  // changing actor behavior + stashing
  // actors can spawn other actors
  // guardians: /system, /user, / = root guardian

  // actors have a defined lifecycle: they can be started, stopped, suspended, resumed, restarted

  // stopping actors - context.stop
  actor ! PoisonPill

  // logging
  // supervision

  // configure Akka infrastructure: dispatchers, routers, mailboxes

  // schedulers
  import system.dispatcher

  import scala.concurrent.duration._
  system.scheduler.scheduleOnce(2 seconds) {
    actor ! "delayed happy birthday!"
  }

  // Akka patterns including FSM + ask pattern
  import akka.pattern.ask
  implicit val timeout = Timeout(3 seconds)

  val future = actor ? "question"

  // the pipe pattern
  import akka.pattern.pipe
  val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")
  future.mapTo[String].pipeTo(anotherActor)
}

object AkkaRecap_Local extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
    """.stripMargin)
    .withFallback(ConfigFactory.load())

  val system = ActorSystem("LocalSystem", config)
  val actorSelection = system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")
  actorSelection ! "Hello from afar!"
}

object AkkaRecap_Remote extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
    """.stripMargin)
    .withFallback(ConfigFactory.load())

  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor")
}

object AkkaRecap_Persistence extends App {
  val config = ConfigFactory.load("persistentStores.conf").getConfig("postgresStore")
  val system = ActorSystem("PersistenceSystem", config)
  val simplePersistentActor = system.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  simplePersistentActor ! "Hello, persistent actor!"
  simplePersistentActor ! "I hope you're persisting what I say..."
}