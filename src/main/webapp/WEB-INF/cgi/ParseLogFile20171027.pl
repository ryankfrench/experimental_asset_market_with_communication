use strict;
use warnings;
use List::Util qw(any first);

my $filename = $ARGV[0];

if(!$filename) {
	print "Usage: perl read_log.pl <Path to Log File> [csv | tab]\n";
	exit(1);
}

my $delim = $ARGV[1] && $ARGV[1] eq "tab" ? "\t" : ",";

###################################

my $globalRound;
my $maxPrice = 700;
my @bids;
my @asks;
my @halfTrades;

sub initializeNewRound {
	$globalRound = shift;
	undef(@bids);
	undef(@asks);
	undef(@halfTrades);
}

sub addBid {
	my $user = shift;
	my $amount = shift;
	push(@bids, {user => $user, amount => $amount});
}

sub findBid {
	my $user = shift;
	my $amount = shift;
	return first { $_->{user} eq $user and $_->{amount} == $amount } @bids;
}

sub removeBid {
	my $user = shift;
	my $amount = shift;
	my $index = first {
		$bids[$_]->{user} eq $user
		and
		$bids[$_]->{amount} == $amount
	} 0..$#bids;
	if(defined($index)) {
		splice(@bids, $index, 1);
	}
}

sub getBestBid {
	return (sort {$b->{amount} <=> $a->{amount}} @bids)[0];
}

sub addAsk {
	my $user = shift;
	my $amount = shift;
	push(@asks, {user => $user, amount => $amount});
}

sub findAsk {
	my $user = shift;
	my $amount = shift;
	return first { $_->{user} eq $user and $_->{amount} == $amount } @asks;
}

sub removeAsk {
	my $user = shift;
	my $amount = shift;
	my $index = first {
		$asks[$_]->{user} eq $user
		and
		$asks[$_]->{amount} == $amount
	} 0..$#asks;
	if(defined($index)) {
		splice(@asks, $index, 1);
	}
}

sub getBestAsk {
	return (sort {$a->{amount} <=> $b->{amount}} @asks)[0];
}

sub cancel {
	my $user = shift;
	my $amount = shift;
	my $askInverted = $maxPrice - $amount;
	my $bid = findBid($user, $amount);
	my $ask = findAsk($user, $askInverted);
	my $askNotInverted = findAsk($user, $amount);
	if($bid) {
		removeBid($user, $amount);
		return "buy"; # success
	} elsif($ask) {
		removeAsk($user, $askInverted);
		return "sell"; # success
	} elsif($askNotInverted) {
		removeAsk($user, $amount);
		return "sellinverted"; # success
	} else {
		return "miss"; # nothing found
	}
}

sub marketToString {
	my $s = "bids:";
	foreach my $bid (@bids) {
		$s .= "{user=$bid->{user};amount=$bid->{amount}}"
	}
	$s .= "|asks:";
	foreach my $ask (@asks) {
		$s .= "{user=$ask->{user};amount=$ask->{amount}}"
	}
	return $s;
}

sub trim {
	my $s = shift;
	if(!$s) {
		$s = "";
	} else {
		$s =~ s/^\s+|\s+$//g;
	}
	return $s;
};

sub error {
	my $rowData = shift; # current row data
	my $errorCode = shift; # error code
	my $lineNumber = $rowData->{lineOfFile};
	my $lineContents = $rowData->{line};
	my $logId = $rowData->{logId};
	$rowData->{action} = "LOG_ERROR";
	$rowData->{e1} = $errorCode;
	$rowData->{e2} = "LogId=$logId";
	$rowData->{e3} = "SortedLineNumber=$lineNumber";
	$rowData->{e4} = trim($lineContents);
	return $rowData;
}

###################################

my $result = open(LOGFILE, $filename);
if(!$result) {
	print "The file ($filename) could not be openned.";
	exit(1);
}

my @fileStream = <LOGFILE>;
my @lines;

# Sort the lines of the file first (natural sort works because of timestamp and Log ID)

foreach my $lineOfFile (@fileStream) {
	push(@lines, $lineOfFile)
}

sub sortByLogId {
	my ($aLineLogId) = ($a =~ / - ([0-9]+)#/);
	my ($bLineLogId) = ($b =~ / - ([0-9]+)#/);
	if($aLineLogId && $bLineLogId) {
		return $aLineLogId <=> $bLineLogId; # sort by ID if both present
	} else {
		return $a cmp $b; # otherwise, use the line (that starts with a timestamp)
	}
}

my @sortedLines = sort sortByLogId @lines;
my $lineOfFile = 0;
my $rowData = {};
my @rows;

foreach my $line (@sortedLines) {

	undef($rowData);
	$lineOfFile += 1;

	# Examples of line handling
	#11/13 09:54:40.243/UTC   WARN -         Book - a2 removed an order for 150
	#11/13 09:53:20.635/UTC   INFO - Session Config - 8609# rounds: 17
	#11/13 01:49:37.954/EST   INFO - ZocaloLogger - 122# ROUND_SCORE_RESULT: USER=a1 CASH=1200 COUPON_VALUE_FOR_ROUND=50 COUPON_COUNT=4 TOTAL_COUPON_VALUE=200 PARTICIPATION_CASH=56 TOTAL_SCORE=1456

	my ($time, $logger, $logId, $message) = ($line =~ /^[0-9\/]+ +([0-9:]{8,8}\.[0-9]{3,3})[^-]+- ([^-]+)- ([0-9# ]+)?(.*)$/);
	$time = trim($time);
	$logger = trim($logger);
	$logId = trim($logId);
	$logId =~ s/#//g;
	$message = trim($message);

	if(trim($line) eq "") {
		next;
	} elsif(!$time || !$logger) {
		$rowData = error($rowData, "COULD_NOT_PARSE");
		$logger = 'LOG_ERROR';
	}

	$rowData = {
		lineOfFile => $lineOfFile,
		line => $line,
		time => $time,
		logId => $logId,
		logger => $logger,
		message => $message,
		round => $globalRound
	};

	if("Session Config" eq $logger) {
		if($message =~ /maxPrice/) {
			($maxPrice) = $message =~ /maxPrice: ([0-9]+)/;
		}

	} elsif("PriceAction" eq $logger) {

		# Trades between buyer and seller

		if($message =~ /had limit offer accepted for -1/) {
			my($buyer, $price) = $message =~ /(.+) had limit offer accepted for .+ at (.+)/;
			push(@halfTrades, { buyer => $buyer, price => $price });
		} elsif($message =~ /accepted offer for/) {
			my($seller, $price) = $message =~ /(.+) accepted offer for .+ at (.+)/;
			my $matchedTrade = first { exists($_->{buyer}) and $_->{price} eq $price } @halfTrades;
			if($matchedTrade) {
				$rowData->{action} = "Sell";
				$rowData->{buyer} = $matchedTrade->{buyer};
				$rowData->{seller} = $seller;
				$rowData->{actor} = $seller;
				$rowData->{price} = $price;
				my $matchIdx = first { exists($halfTrades[$_]->{buyer}) and $halfTrades[$_]->{price} eq $price } 0..$#halfTrades;
				splice(@halfTrades, $matchIdx, 1);
				removeBid($matchedTrade->{buyer}, $price);
			} else {
				$rowData = error($rowData, "BAD_HALF_TRADE");
			}
		} elsif($message =~ /had limit offer accepted for 0/) {
			next; # cancel/action at near same time
		} elsif($message =~ /had limit offer accepted at/) {
			my($seller, $price) = $message =~ /(.+) had limit offer accepted at (.+)/;
			push(@halfTrades, { seller => $seller, price => $price });
		} elsif($message =~ /accepted offer/) {
			my($buyer, $price) = $message =~ /(.+) accepted offer at (.+)/;
			my $matchedTrade = first { exists($_->{seller}) and $_->{price} eq $price } @halfTrades;
			if($matchedTrade) {
				$rowData->{action} = "Buy";
				$rowData->{seller} = $matchedTrade->{seller};
				$rowData->{buyer} = $buyer;
				$rowData->{actor} = $buyer;
				$rowData->{price} = $price;
				my $matchIdx = first { exists($halfTrades[$_]->{seller}) and $halfTrades[$_]->{price} eq $price } 0..$#halfTrades;
				splice(@halfTrades, $matchIdx, 1);
				removeAsk($matchedTrade->{seller}, $price);
			} else {
				$rowData = error($rowData, "BAD_HALF_TRADE");
			}

		# Limit orders (bids and asks)

		} elsif($message =~ /added Buy at/) {
			my($buyer, $price) = $message =~ /(.+) added Buy at ([0-9]+)/;
			addBid($buyer, $price);
			$rowData->{action} = "added Buy";
			$rowData->{actor} = $buyer;
			$rowData->{price} = $price;
		} elsif($message =~ /added Sell at/) {
			my($seller, $price) = $message =~ /(.+) added Sell at ([0-9]+)/;
			addAsk($seller, $price);
			$rowData->{action} = "added Sell";
			$rowData->{actor} = $seller;
			$rowData->{price} = $price;

		# Cancellations

		} elsif($message =~ /cancelled [oO]rder at/) {
			my($user, $price) = $message =~ /(.+) cancelled [oO]rder at ([0-9]+)/;
			my $result = cancel($user, $price);
			if($result eq "buy") {
				$rowData->{action} = "Cancel Buy Order";
				$rowData->{actor} = $user;
				$rowData->{price} = $price;
			} elsif($result eq "sell") {
				$rowData->{action} = "Cancel Sell Order";
				$rowData->{actor} = $user;
				$rowData->{price} = $maxPrice - $price;
			} elsif($result eq "sellinverted") {
				$rowData->{action} = "Cancel Sell Order";
				$rowData->{actor} = $user;
				$rowData->{price} = $price;
			} elsif($result eq "conflict") {
				$rowData->{action} = "Cancel Buy Order";
				$rowData->{actor} = $user;
				$rowData->{price} = $price;
				$rowData->{e1} = "This user had conflicting bid/ask.  Assuming bid cancelled.";
			} else {
				$rowData = error($rowData, "UNMATCHED_CANCELLATION");
			}
		}

	# New rounds

	} elsif("Transition" eq $logger) {
		if($message =~ /State transition: startRound/) {
			my ($newRound) = $message =~ /State transition: startRound, round: ([0-9]+)./;
			initializeNewRound($newRound);
		}

	# Scores for Round - Backwards Compatible for Older Files

	} elsif("Score" eq $logger) {
		if($message =~ /dividend/) {
			my ($user, $dividend, $dividendPaid, $score, $coupons) = $message =~ /(.+) dividend: ([0-9]+) dividendPaid: ([0-9]+) score: ([0-9]+) coupons: ([0-9]+)/;
			$rowData->{action} = "dividend";
			$rowData->{actor} = $user;
			$rowData->{price} = $dividendPaid;
			$rowData->{e1} = 'Dividend=' . ($dividend || '');
			$rowData->{e2} = 'Coupons=' . ($coupons || '');
			$rowData->{e3} = 'TotalDividend=' . ($dividendPaid || '');
			$rowData->{e4} = 'Score=' . ($score || '');
		}

	} elsif("ZocaloLogger" eq $logger) {

		# Scores for Round - New Way

		if ($message =~ /ROUND_SCORE_RESULT/ ) {
			my ($user, $cash, $couponValue, $couponCount, $totalCouponValue, $participationCash, $totalScore) = $message =~ /ROUND_SCORE_RESULT: USER=(.*) CASH=(.*) COUPON_VALUE_FOR_ROUND=(.*) COUPON_COUNT=(.*) TOTAL_COUPON_VALUE=(.*) PARTICIPATION_CASH=(.*) TOTAL_SCORE=(.*)/;
			$rowData->{action} = "dividend";
			$rowData->{actor} = $user;
			$rowData->{price} = $totalScore;
			$rowData->{e1} = 'Cash=' . ($cash || '');
			$rowData->{e2} = 'Coupons=' . ($couponCount || '');
			$rowData->{e3} = 'ParticipationCash=' . ($participationCash || '');
			$rowData->{e4} = 'CouponValue=' . ($couponValue || '');
			$rowData->{e5} = 'Score=' . ($totalScore || '');

		# New Round (old way still works, so ignore)

		} elsif ($message =~ /NEW_ROUND_STARTED/ ) {
			next;

		# Clues assigned (auto assignment)

		} elsif ($message =~ /CLUE_ASSIGNMENT/ ) {
			my ($round, $user, $clue) = $message =~ /CLUE_ASSIGNMENT: ROUND=(.*) ID=(.*) CLUE=(.*)/;
			$rowData->{action} = "Clue Assignment";
			$rowData->{round} = $round; # Occurs before next round starts
			$rowData->{actor} = $user;
			$rowData->{e1} = ($clue || '');

		# Participation Choices

		} elsif ($message =~ /PARTICIPATION_CHOICE/ ) {
			my ($round, $user, $participate, $clue, $extraClues, $freeClues, $participationCash) = $message =~ /PARTICIPATION_CHOICE: ROUND=(.*) ID=(.*) PARTICIPATE=(.*) CLUE=(.*) ADDITIONAL_CLUES=(.*) FREE_CLUES=(.*) PARTICIPATION_CASH=(.*)/;
			$rowData->{action} = "Participation Choice";
			$rowData->{round} = $round; # Occurs before next round starts
			$rowData->{actor} = $user;
			$rowData->{e1} = 'Participate=' . $participate;
			$rowData->{e2} = 'ReceiveClue=' . $clue;
			$rowData->{e3} = 'ParticipationCash=' . $participationCash;
			$rowData->{e4} = 'AdditionalClues=' . $extraClues;
			$rowData->{e5} = 'FreeClues=' . $freeClues;

		} elsif ($message =~ /CHAT_COLOR_ASSIGNMENT/ ) {
			my ($user, $colors) = $message =~ /CHAT_COLOR_ASSIGNMENT FOR (.*): \{(.*)\}/;
			$colors = join(';', split(', ', $colors));
			$rowData->{action} = "Color Assignment";
			$rowData->{actor} = $user;
			$rowData->{e1} = 'Colors=' . ($colors || '');

		# Chat messages sent by participants

		} elsif ($message =~ /CHAT_MESSAGE/ ) {
			my ($sender, $recipients, $message) = $message =~ /CHAT_MESSAGE: FROM=(.*) TO=\[(.*)\] MSG=(.*)/;
			$message =~ s/^\s*(.*?)\s*$/$1/g; # trim
			$message =~ s/,/~/g; # remove commas
			$message =~ s/\s+/ /g; # consolidate spaces
			$recipients =~ s/^\s*(.*?)\s*$/$1/g; # trim
			$recipients =~ s/,/\//g; # remove commas
			$recipients =~ s/\s+//g; # consolidate spaces
			$rowData->{action} = "Chat";
			$rowData->{actor} = $sender;
			$rowData->{e1} = 'Recipients=' . ($recipients || '');
			$rowData->{e2} = 'Message=' . ($message || '');

		# One participant blocks another

		} elsif ($message =~ /CHAT_BLOCK/ ) {
			my ($blocker, $blocked) = $message =~ /CHAT_BLOCK: USER=(.*) AFFECTED=(.*)/;
			$blocker =~ s/\s+/ /g; # consolidate spaces
			$blocked =~ s/\s+/ /g; # consolidate spaces
			$rowData->{action} = "Blocked";
			$rowData->{actor} = $blocker;
			$rowData->{buyer} = $blocker;
			$rowData->{seller} = $blocked;
			$rowData->{e1} = 'Blocker=' . ($blocker || '');
			$rowData->{e2} = 'Blocked=' . ($blocked || '');

		# One participant blocks another

        } elsif ($message =~ /CHAT_UNBLOCK/ ) {
			my ($blocker, $unblocked) = $message =~ /CHAT_UNBLOCK: USER=(.*) AFFECTED=(.*)/;
			$blocker =~ s/\s+/ /g; # consolidate spaces
			$unblocked =~ s/\s+/ /g; # consolidate spaces
			$rowData->{action} = "Unblocked";
			$rowData->{actor} = $blocker;
			$rowData->{buyer} = $blocker;
			$rowData->{seller} = $unblocked;
			$rowData->{e1} = 'Blocker=' . ($blocker || '');
			$rowData->{e2} = 'Unblocked=' . ($unblocked || '');

		}

	} elsif(any {$_ eq $logger} ('UserError', 'Book', 'Session')) {
		next;

	} else {
		$rowData = error($rowData, "UKNOWN_COMMAND");
	}

	# Add the best bid/ask to rows that should be printed

	if($rowData->{action}) {
		my $bestBid = getBestBid();
		my $bestAsk = getBestAsk();
		$rowData->{bid} = $bestBid->{amount} || 0;
		$rowData->{ask} = $bestAsk->{amount} || $maxPrice;
		$rowData->{fullMarket} = marketToString();
		push(@rows, $rowData);
	}

}

# Print out rows to file

print	"SessionID", $delim,
		"Time", $delim,
		"Event", $delim,
		"Round", $delim,
		"Actor", $delim,
		"Action", $delim,
		"Quant", $delim,
		"Price", $delim,
		"Buyer", $delim,
		"Seller", $delim,
		"BestBid", $delim,
		"BestAsk", $delim,
		"Role", $delim,
		"Extra1", $delim,
		"Extra2", $delim,
		"Extra3", $delim,
		"Extra4", $delim,
		"Extra5", $delim,
		"Extra6", $delim,
		"FullMarket", "\n";

foreach my $r (@rows) {

	my $time = $r->{time} || '';
	my $logId = $r->{logId} || '';
	my $round = $r->{round} || 0;
	my $actor = $r->{actor} || '';
	my $action = $r->{action} || '';
	my $price = $r->{price} || '';
	my $buyer = $r->{buyer} || '';
	my $seller = $r->{seller} || '';
	my $bid = $r->{bid} || 0;
	my $ask = $r->{ask} || $maxPrice;
	my $e1 = $r->{e1} || '';
	my $e2 = $r->{e2} || '';
	my $e3 = $r->{e3} || '';
	my $e4 = $r->{e4} || '';
	my $e5 = $r->{e5} || '';
	my $e6 = $r->{e6} || '';
	my $fullMarket = $r->{fullMarket} || '';

	my $quant = 1;
	if($action eq "Sell") {
		$quant = -1;
	} elsif($action eq "dividend") {
		$quant = "";
	}

	print	$filename, $delim, # global
			$time, $delim,
			$logId, $delim,
			$round, $delim,
			$actor, $delim,
			$action, $delim,
			$quant, $delim, # calculated above
			$price, $delim,
			$buyer, $delim,
			$seller, $delim,
			$bid, $delim,
			$ask, $delim,
			"trader", $delim, # always trader for now
			$e1, $delim,
			$e2, $delim,
			$e3, $delim,
			$e4, $delim,
			$e5, $delim,
			$e6, $delim,
			$fullMarket, "\n";

}

