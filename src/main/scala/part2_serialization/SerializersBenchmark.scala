package part2_serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.serialization.Serializer
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import com.typesafe.config.ConfigFactory
import part2_serialization.Datamodel.ProtobufVote

import scala.util.Random

// voting scenario

case class Vote(ssn: String, candidate: String)
case object VoteEnd

object VoteGenerator {
  val random = new Random()
  val candidates = List("alice", "bob", "charlie", "deedee")

  def getRandomCandidate = candidates(random.nextInt(candidates.length))

  def generateVotes(count: Int) = (1 to count).map(_ => Vote(UUID.randomUUID().toString, getRandomCandidate))

  def generateProtobufVotes(count: Int) = (1 to count).map { _ =>
    ProtobufVote.newBuilder()
      .setSsn(UUID.randomUUID().toString)
      .setCandidate(getRandomCandidate)
      .build()
  }
}

class VoteAvroSerializer extends Serializer {

  val voteSchema = AvroSchema[Vote]

  override def identifier: Int = 52375

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case vote: Vote  =>
      val baos = new ByteArrayOutputStream()
      val avroOutputStream = AvroOutputStream.binary[Vote].to(baos).build(voteSchema)
      avroOutputStream.write(vote)
      avroOutputStream.flush()
      avroOutputStream.close()
      baos.toByteArray
    case _ => throw new IllegalArgumentException("We only support votes in this benchmark serializer")
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val inputStream = AvroInputStream.binary[Vote].from(new ByteArrayInputStream(bytes)).build(voteSchema)
    val voteIterator: Iterator[Vote] = inputStream.iterator
    val vote = voteIterator.next()
    inputStream.close()

    vote
  }

  override def includeManifest: Boolean = true
}

class VoteAggregator extends Actor with ActorLogging {
  override def receive: Receive = ready

  def ready: Receive = {
    case _: Vote | _: ProtobufVote => context.become(online(1, System.currentTimeMillis()))
  }

  def online(voteCount: Int, originalTime: Long): Receive = {
    case _: Vote | _: ProtobufVote =>
      context.become(online(voteCount + 1, originalTime))
    case VoteEnd =>
      val duration = (System.currentTimeMillis() - originalTime) * 1.0 / 1000
      log.info(f"Received $voteCount votes in $duration%5.3f seconds")
      context.become(ready)
  }
}

object VotingStation extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
    """.stripMargin)
    .withFallback(ConfigFactory.load("serializersBenchmark"))

  val system = ActorSystem("VotingStation", config)
  val actorSelection = system.actorSelection("akka://VotingCentralizer@localhost:2552/user/voteAggregator")

  val votes = VoteGenerator.generateProtobufVotes(1000000)
  votes.foreach(actorSelection ! _)
  actorSelection ! VoteEnd
}

object VotingCentralizer extends App {
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
    """.stripMargin)
    .withFallback(ConfigFactory.load("serializersBenchmark"))

  val system = ActorSystem("VotingCentralizer", config)
  val votingAggregator = system.actorOf(Props[VoteAggregator], "voteAggregator")
}


/**
  * TIME RESULTS:
  *
  * 100K
  *   - java 2.110
  *   - avro 2.114
  *   - kryo 1.178
  *   - protobuf 1.100
  *
  * 1M
  *   - java 12.729
  *   - avro 9.971
  *   - kryo 6.813
  *   - protobuf 6.596
  */


object SerializersBenchmark_Persistence extends App {
  val config = ConfigFactory.load("persistentStores").getConfig("postgresStore")
    .withFallback(ConfigFactory.load("serializersBenchmark"))

  val system = ActorSystem("PersistenceSystem", config)

  val simplePersistentActor = system.actorOf(SimplePersistentActor.props("benchmark-protobuf", false), "benchmark")
  val votes = VoteGenerator.generateProtobufVotes(10000)
  votes.foreach(simplePersistentActor ! _)
}

/**
  * MEM TEST RESULTS:
  *
  *          avg          |   persistence_id
  * ----------------------+--------------------
  *  201.2509000000000000 | benchmark-java
  *  134.2436000000000000 | benchmark-avro
  *  156.2254000000000000 | benchmark-protobuf
  *  108.2505000000000000 | benchmark-kryo
  */