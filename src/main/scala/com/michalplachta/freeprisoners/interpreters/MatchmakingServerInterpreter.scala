package com.michalplachta.freeprisoners.interpreters

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import cats.~>
import com.michalplachta.freeprisoners.PrisonersDilemma.Prisoner
import com.michalplachta.freeprisoners.actors.MatchmakingServerActor._
import com.michalplachta.freeprisoners.algebras.MatchmakingOps.Matchmaking.WaitingPlayer
import com.michalplachta.freeprisoners.algebras.MatchmakingOps._
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MatchmakingServerInterpreter extends (Matchmaking ~> Future) {
  private val system = ActorSystem("matchmakingClient")
  private val config = ConfigFactory.load
  private val maxRetries = config.getInt("app.matchmaking-client.max-retries")
  private val retryTimeout = Timeout(
    config.getDuration("app.matchmaking-client.retry-timeout").toMillis,
    TimeUnit.MILLISECONDS)

  private val server =
    system.actorSelection(config.getString("app.game-server-path"))

  def apply[A](matchmaking: Matchmaking[A]): Future[A] = matchmaking match {
    case RegisterAsWaiting(player) =>
      tellServer(server, AddToWaitingList(player.name))
    case UnregisterPlayer(player) =>
      tellServer(server, RemoveFromMatchmaking(player.name))
    case GetWaitingPlayers() =>
      askServer(server, GetWaitingList(), maxRetries, retryTimeout)
        .map(_.map(name => WaitingPlayer(Prisoner(name))))
    case JoinWaitingPlayer(player, waitingPlayer) =>
      tellServer(server,
                 RegisterMatch(player.name, waitingPlayer.prisoner.name))
      askServer(server, GetOpponentName(player.name), maxRetries, retryTimeout)
        .map(_.map(Prisoner))
    case WaitForOpponentToJoin(player, maxWaitTime) =>
      askServer(server,
                GetOpponentName(player.name),
                (maxWaitTime / retryTimeout.duration).toInt,
                retryTimeout).map(_.map(Prisoner))
  }

  def terminate(): Unit = system.terminate()
}
