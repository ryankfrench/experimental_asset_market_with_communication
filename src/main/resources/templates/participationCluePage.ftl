<h1>Clue</h1>
<p>You have elected to participate in the market for this period.</p>
<#if chatState.roundParticipationCost?? >
	<p>Your current balance is ${balance + chatState.roundParticipationCost} Francs.</p>
<#else>
	<p>Your current balance is ${balance} Francs.</p>
</#if>
<p>You have the option to purchase information for ${-chatState.roundClueCost} francs.</p>
<form>
	<p><input type="radio" name="radioClue" value="true"/> Yes.  I would like to purchase a clue and will pay ${-chatState.roundClueCost} Francs.</p>
	<p><input type="radio" name="radioClue" value="false"/> No.  I do not want a clue.</p>
	<p><input type="button" value="Submit" onclick="receiveClue();"></p>
</form>