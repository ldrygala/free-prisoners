package com.michalplachta.freeprisoners.actors

import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.TestKit
import com.michalplachta.freeprisoners.actors.MatchmakingServerActor._
import com.michalplachta.freeprisoners.actors.ServerCommunication._
import org.scalatest.{AsyncWordSpecLike, Matchers}

import scala.concurrent.duration._

class MatchmakingServerActorTest
    extends TestKit(ActorSystem("matchmakingTest"))
    with AsyncWordSpecLike
    with Matchers {
  "Matchmaking server actor" should {
    "add player names to the waiting list" in {
      val server = createServer()
      tellServer(server, AddToWaitingList("a"))
      tellServer(server, AddToWaitingList("b"))
      askServer(server, GetWaitingList(), 1, 1.second)
        .map(_ should be(Set("a", "b")))
    }

    "remove player name from the waiting list when it's removed from matchmaking" in {
      val server = createServer()
      tellServer(server, AddToWaitingList("a"))
      tellServer(server, AddToWaitingList("b"))
      tellServer(server, RemoveFromMatchmaking("a"))
      askServer(server, GetWaitingList(), 1, 1.second)
        .map(_ should be(Set("b")))
    }

    "respond with the opponent after match is registered" in {
      val server = createServer()
      tellServer(server, RegisterMatch("a", "b"))
      askServer(server, GetOpponentName("a"), 1, 1.second)
        .map(_ should contain("b"))

      askServer(server, GetOpponentName("b"), 1, 1.second)
        .map(_ should contain("a"))
    }

    "respond with no opponent when the player is only on the waiting list" in {
      val server = createServer()
      tellServer(server, AddToWaitingList("a"))
      askServer(server, GetOpponentName("a"), 1, 1.second)
        .map(_ should be(None))
    }

    "remove player names from the registered matches when one of them is removed from matchmaking" in {
      val server = createServer()
      tellServer(server, RegisterMatch("a", "b"))
      tellServer(server, RemoveFromMatchmaking("a"))

      askServer(server, GetOpponentName("a"), 1, 1.second)
        .map(_ should be(None))

      askServer(server, GetOpponentName("b"), 1, 1.second)
        .map(_ should be(None))
    }
  }

  private def createServer() =
    ActorSelection(system.actorOf(Props[MatchmakingServerActor]), "/")
}
