package com.michalplachta.freeprisoners.programs

import cats.free.Free
import com.michalplachta.freeprisoners.PrisonersDilemma.{
  Guilty,
  Prisoner,
  Silence,
  Verdict
}
import com.michalplachta.freeprisoners.algebras.GameOps.Game
import com.michalplachta.freeprisoners.algebras.MatchmakingOps._
import com.michalplachta.freeprisoners.algebras.PlayerOps.Player
import com.michalplachta.freeprisoners.interpreters.GameTestInterpreter.GameState
import com.michalplachta.freeprisoners.interpreters.MatchmakingTestInterpreter.MatchmakingState
import com.michalplachta.freeprisoners.interpreters.PlayerGameTestInterpreter.{
  PlayerGame,
  PlayerGameState
}
import com.michalplachta.freeprisoners.interpreters.PlayerTestInterpreter.PlayerState
import com.michalplachta.freeprisoners.interpreters.{
  MatchmakingTestInterpreter,
  PlayerGameTestInterpreter
}
import com.michalplachta.freeprisoners.programs.Multiplayer.findOpponent
import org.scalatest.{Matchers, WordSpec}

class MultiplayerTest extends WordSpec with Matchers {
  "Multiplayer game" should {
    "have matchmaking module which" should {
      implicit val matchmakingOps: Matchmaking.Ops[Matchmaking] =
        new Matchmaking.Ops[Matchmaking]

      "be able to create a match when there is one opponent registered" in {
        val player = Prisoner("Player")
        val registeredOpponent = Prisoner("Opponent")

        val program: Free[Matchmaking, Option[Prisoner]] = for {
          _ <- matchmakingOps.registerAsWaiting(registeredOpponent)
          opponent <- findOpponent(player)
        } yield opponent

        val opponent: Option[Prisoner] = program
          .foldMap(new MatchmakingTestInterpreter)
          .runA(MatchmakingState(Set.empty, Set.empty))
          .value

        opponent should contain(Prisoner("Opponent"))
      }

      "not be able to create a match when there are no opponents" in {
        val player = Prisoner("Player")

        val opponent: Option[Prisoner] = findOpponent(player)
          .foldMap(new MatchmakingTestInterpreter)
          .runA(MatchmakingState(Set.empty, Set.empty))
          .value

        opponent should be(None)
      }

      "keep count of registered and unregistered players" in {
        val player = Prisoner("Player")

        val state: MatchmakingState = findOpponent(player)
          .foldMap(new MatchmakingTestInterpreter)
          .runS(MatchmakingState(Set.empty, Set.empty))
          .value

        state.waitingPlayers.size should be(0)
        state.metPlayers should be(Set(player))
      }
    }

    "have game module which" should {
      "is able to produce verdict if both players make decisions" in {
        val player = Prisoner("Player")
        val opponent = Prisoner("Opponent")

        val initialState =
          PlayerGameState(PlayerState(Set.empty,
                                      Map(player -> Guilty),
                                      Map.empty),
                          GameState(Map(opponent -> Silence)))
        val result: PlayerGameState = Multiplayer
          .playTheGame(player, opponent)(new Player.Ops[PlayerGame],
                                         new Game.Ops[PlayerGame])
          .foldMap(new PlayerGameTestInterpreter)
          .runS(initialState)
          .value

        result.playerState.verdicts.get(player) should contain(Verdict(0))
      }

      "is not able to produce verdict if the opponent doesn't make a decision" in {
        val player = Prisoner("Player")
        val opponent = Prisoner("Opponent")

        val initialState =
          PlayerGameState(PlayerState(Set.empty,
                                      Map(player -> Guilty),
                                      Map.empty),
                          GameState(Map.empty))

        val result: PlayerGameState = Multiplayer
          .playTheGame(player, opponent)(new Player.Ops[PlayerGame],
                                         new Game.Ops[PlayerGame])
          .foldMap(new PlayerGameTestInterpreter)
          .runS(initialState)
          .value

        result.playerState.verdicts should be(Map.empty)
      }
    }
  }
}
