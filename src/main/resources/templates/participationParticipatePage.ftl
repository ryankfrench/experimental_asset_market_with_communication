<h1>Participation</h1>
<p>You have the option to participate in a one period market in which you will have the opportunity to earn money.</p>
<p>Your initial cash balance for this period is ${balance} francs.</p>
<#if chatState.roundParticipationCost == 0 >
	<p>There is no cost to participate in the market.</p>
<#elseif chatState.roundParticipationCost < 0 >
	<p>There is a cost to participate in the market.  To participate you must pay ${-chatState.roundParticipationCost} francs.</p>
<#else>
	<p>You will receive ${chatState.roundParticipationCost} francs if you participate.</p>
</#if>
<p>If you choose to not participate, then you will be able to browse the internet during this period.</p>
<#if chatState.roundClueCost??>
	<p>Moreover, on the next screen you will have the opportunity to acquire information to help you understand the value of the asset.  If you decide to purchase information, then it will cost you ${-chatState.roundClueCost} francs.</p>
</#if>
<form>
	<#if chatState.roundParticipationCost == 0 >
		<p><input type="radio" name="radioParticipate" value="true"/> Yes.  I will participate.</p>
	<#elseif chatState.roundParticipationCost < 0 >
		<p><input type="radio" name="radioParticipate" value="true"/> Yes.  I will participate and pay ${-chatState.roundParticipationCost} Francs.</p>
	<#else>
		<p><input type="radio" name="radioParticipate" value="true"/> Yes.  I will participate and receive ${chatState.roundParticipationCost} Francs.</p>
	</#if>
	<p><input type="radio" id="radioParticipate" name="radioParticipate" value="false"/> No.  I do not wish to participate.</p>
	<p><input type="button" value="Submit" onclick="participate();"></p>
</form>