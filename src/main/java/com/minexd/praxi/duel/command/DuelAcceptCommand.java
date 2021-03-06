package com.minexd.praxi.duel.command;

import com.minexd.praxi.arena.Arena;
import com.minexd.praxi.duel.DuelRequest;
import com.minexd.praxi.match.Match;
import com.minexd.praxi.match.impl.BasicTeamMatch;
import com.minexd.praxi.match.participant.MatchGamePlayer;
import com.minexd.praxi.participant.GameParticipant;
import com.minexd.praxi.participant.TeamGameParticipant;
import com.minexd.praxi.party.Party;
import com.minexd.praxi.profile.Profile;
import com.minexd.zoot.util.CC;
import com.qrakn.honcho.command.CPL;
import com.qrakn.honcho.command.CommandMeta;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandMeta(label = "duel accept")
public class DuelAcceptCommand {

	public void execute(Player player, @CPL("player") Player target) {
		if (target == null) {
			player.sendMessage(CC.RED + "That player is no longer online.");
			return;
		}

		if (player.hasMetadata("frozen")) {
			player.sendMessage(CC.RED + "You cannot duel while frozen.");
			return;
		}

		if (target.hasMetadata("frozen")) {
			player.sendMessage(CC.RED + "You cannot duel a frozen player.");
			return;
		}

		Profile playerProfile = Profile.getByUuid(player.getUniqueId());

		if (playerProfile.isBusy()) {
			player.sendMessage(CC.RED + "You cannot duel right now.");
			return;
		}

		Profile targetProfile = Profile.getByUuid(target.getUniqueId());

		if (targetProfile.isBusy()) {
			player.sendMessage(target.getDisplayName() + CC.RED + " is currently busy.");
			return;
		}

		DuelRequest duelRequest = playerProfile.getDuelRequest(target);

		if (duelRequest != null) {
			if (targetProfile.isDuelRequestExpired(duelRequest)) {
				player.sendMessage(CC.RED + "That duel request has expired!");
				return;
			}

			if (duelRequest.isParty()) {
				if (playerProfile.getParty() == null) {
					player.sendMessage(CC.RED + "You do not have a party to duel with.");
					return;
				} else if (targetProfile.getParty() == null) {
					player.sendMessage(CC.RED + "That player does not have a party to duel with.");
					return;
				}
			} else {
				if (playerProfile.getParty() != null) {
					player.sendMessage(CC.RED + "You cannot duel whilst in a party.");
					return;
				} else if (targetProfile.getParty() != null) {
					player.sendMessage(CC.RED + "That player is in a party and cannot duel right now.");
					return;
				}
			}

			Arena arena = duelRequest.getArena();

			if (arena.isActive()) {
				arena = Arena.getRandomArena(duelRequest.getKit());
			}

			if (arena == null) {
				player.sendMessage(CC.RED + "Tried to start a match but there are no available arenas.");
				return;
			}

			playerProfile.getDuelRequests().remove(duelRequest);

			arena.setActive(true);

			GameParticipant<MatchGamePlayer> participantA = null;
			GameParticipant<MatchGamePlayer> participantB = null;

			if (duelRequest.isParty()) {
				for (Party party : new Party[]{ playerProfile.getParty(), targetProfile.getParty() }) {
					Player leader = party.getLeader();
					MatchGamePlayer gamePlayer = new MatchGamePlayer(leader.getUniqueId(), leader.getName());
					TeamGameParticipant<MatchGamePlayer> participant = new TeamGameParticipant<>(gamePlayer);

					for (Player partyPlayer : party.getListOfPlayers()) {
						if (!partyPlayer.getPlayer().equals(leader)) {
							participant.getPlayers().add(new MatchGamePlayer(partyPlayer.getUniqueId(),
									partyPlayer.getName()));
						}
					}

					if (participantA == null) {
						participantA = participant;
					} else {
						participantB = participant;
					}
				}
			} else {
				MatchGamePlayer playerA = new MatchGamePlayer(player.getUniqueId(), player.getName());
				MatchGamePlayer playerB = new MatchGamePlayer(target.getUniqueId(), target.getName());

				participantA = new GameParticipant<>(playerA);
				participantB = new GameParticipant<>(playerB);
			}

			Match match = new BasicTeamMatch(null, duelRequest.getKit(), arena, false, participantA, participantB);
			match.start();
		} else {
			player.sendMessage(CC.RED + "You do not have a duel request from that player.");
		}
	}

}
