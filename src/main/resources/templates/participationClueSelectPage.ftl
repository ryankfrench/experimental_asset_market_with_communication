<#assign startingClues = config.getParticipationSelectStartingForRound(chatState.participationRound) />
<#assign maxClues = config.getParticipationSelectMaximumForRound(chatState.participationRound) />
<#assign cluesToBuy = (maxClues - startingClues) />

<h1>Clue</h1>
<p>You have elected to participate in the market for this period.</p>
<#if chatState.roundParticipationCost?? >
    <p>Your current balance is ${balance + chatState.roundParticipationCost} Francs.</p>
<#else>
    <p>Your current balance is ${balance} Francs.</p>
</#if>
<p>You have the option to purchase information for ${-chatState.roundClueCost} francs.</p>
<p>For this round, you will start with ${startingClues} clues for the round, and can buy up to ${cluesToBuy} additional clues for a total of ${maxClues} clues.</p>
<p>How many additional clues would you like to purchase?</p>
<form>
	<p>
		<select id="multiClueSelect">
			<option value="-1"></option>
			<#list 0..cluesToBuy as c>
				<option value="${c}">${c}</option>
			</#list>
		</select>
		Additional Clues
	</p>
	<p><input type="button" value="Submit" onclick="receiveSelectClue();"></p>
</form>