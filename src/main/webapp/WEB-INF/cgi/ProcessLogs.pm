#!/usr/bin/env perl

#Copyright 2007-2009 Chris Hibbert.  All rights reserved.
#Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.
#
#This software is published under the terms of the MIT license, a copy of
#which has been included with this distribution in the LICENSE file.

### unhandled: endowment, tickets

package ProcessLogs;
use Exporter;
@ISA = ('Exporter');
@EXPORT = ('processLines', 'printTitles');
@EXPORT_OK = ('getRole', 'getBestBid', 'getBestAsk', 'getRound');

use strict;

my %book;         # open offers hash price by name
my @actualValues; # Ticket Value by round
my %roles;        # role indexed by name
my %hints;        # list of hints indexed by name
my %targets;    # targets indexed by manipulator's name
my $round = 0;
my $bestBid = 0;
my @posClues = 0;        # Number of traders who received positive clues
my @posTargets = 0;    # Number of manipulators who have positive targets
my $maxPrice = 100;
my $bestAsk = $maxPrice;
my %mmPrice;
my $sessionType;
my $betterPriceRequired;
my $strict = $ENV{'STRICT_CHECKING'};
my $openMarketCount = 0;        #  If there are always zero or one open market then position is distinct
my $useDistinctNames = 0;       # if there are more than 1, we'll use '$market:$position'
my $goalAction;  # current
my $goalIssue;  # current
my $costLimit;  # current
my $priceTarget;  # current
my $nextOffer = "";  #Book orders are sorted arbitrarily.  nextOffer tells whose order will be accepted next
my $beta;

sub processLines {
        my ($time, $msecIgnore, $zoneIgnore, $seqIgnore, $seqId) = ($_ =~ / (..:..:..(\.[0-9]+)?)(\/[A-Z]+)? +[A-Z]+ -.[^-]+- (([0-9]+)# )?/);
        if (/PriceAction - /) {
            my ($name, $command, $price, $quant, $ignore);
            if (/PriceAction - ([0-9]+# )?(.+) at ([-.0-9]+)/) {
                ($ignore, $name, $command, $price) = ($_ =~ /PriceAction - ([0-9]+# )?([-\w]*|[-\w]*'s liquidator) (.+) at ([-.0-9]+)/);
#print "PriceAction: i: $seqId, n: $name, c: $command, p: $price\n";
             ($quant, $ignore) = ($command =~ /for ([-\w]*)( of [-\w]*)*( at [-\w]*)*/);
                if ($quant eq "") {
                    $quant = 1;
                }
            } elsif (/PriceAction - ([0-9]+# )?(.+) to ([-.0-9]+)/) {
                ($name, $command, $quant, $price) = ($_ =~ /PriceAction - [0-9]+# ([-\w]*|[-\w]*'s liquidator) (.+) for (.+) of .*:.* to ([-.0-9]+)/);
#print "PriceAction: i: $seqId, n: $name, p: $price, q: $quant\n";
            } elsif (/PriceAction - ([0-9]+# )?(.+) for 0 of .+ at .$/) {
                ($name, $command, $quant, $price) = ($_ =~ /PriceAction - [0-9]+# ([-\w]*|[-\w]*'s liquidator) (traded.+maker) for (.) of .*:.* .+ at .$/);
#print "PriceAction: i: $seqId, n: $name, p: $price\n";
            } else {
print "Unable to parse PriceAction line.\n";
            }

#print "PriceAction i: $seqId, n: $name, c: $command, p: $price, q: $quant\n";
                if ($command =~ /best/) {
     # ignore
                } elsif ($command =~ /redeemed/) {
                    my ($quantRedeemed, $market, $position) = ($command =~ /redeemed (.*) coupons +for (.+):(.+)/);
                    redeem($time, $seqId, $name, $quantRedeemed, $price, $market, $position);
                } elsif ($command =~ /added/) {
                    newOffer($time, $seqId, $name, $quant, $price, $command);
                } elsif ($command =~ /accepted/) {
                    if ($command =~ /accepted offer/) {
                        accepted($time, $seqId, $name, $quant, $price);
                    } elsif ($command =~ /offer accepted/) {
#print "PriceAction i: $seqId, n: $name, c: $command, p: $price, q: $quant\n";
                        $nextOffer = $name;
#printBook();
                    }
                } elsif ($command =~ /cancelled/) {
                        cancel($time, $seqId, $name, $quant, $price);
                } elsif ($command =~ /traded with/) {
                    my ($market, $position, $startPrice, $endPrice) = ($_ =~ /PriceAction - .* for [-0-9.]+ of (.*):([-\w]*) .*from ([-.0-9]+) to ([-.0-9]+)/);
                    if ($startPrice != "") {
                        my $mmPrice = mmPrice($market, $position);
                        if ($mmPrice == "") {
                            setMmPrice($market, $position, $startPrice);
                        } elsif ($mmPrice != $startPrice && $mmPrice != "") {
                            print "mm price inconsistent: i: $seqId, should have started from $mmPrice, but start was $startPrice\n";
                        }

                        makerTrade($time, $seqId, $name, $quant, $price, $market, $position);
                        setMmPrice($market, $position, $endPrice);
                    }
                } else {
                    if ($strict) {
                        print " MISSED PriceAction: '$name' ($command) [$quant]" , "\n";
                    }
                }
        } elsif (/ UserError / | / Resetting Order Book / | / endTrading/ | / Display Scores/ | / endRound/ |
                         / removed an order / | /creating Account/ | / votingDone/ | / SessionState / ) {
                # noop
        } elsif ( / Start next round/ | / endScoring/ ) {
                # noop
        } elsif (/ Config / | / Session /) {
                handleConfig($_);
        } elsif ( / estimate: / ) {
                my ($ignore, $judge, $value) = ($_ =~ /Score - ([0-9]*)?# ([-\w]*) estimate: ([0-9.]+)/);
                printEstimate($time, $seqId, $judge, $value);
        } elsif ( / loans: / ) {
                my ($ignore, $borrower, $dIgnore, $ign, $default, $bal, $loan) = ($_ =~ /Score - ([0-9]*)?# ([-\w]*) ((Defaulted|loan.*reduced by):* ([-0-9.]+) )*balance: ([0-9.]+) loans: ([0-9.]+)/);
#print("LOANS:  $borrower, $time, $dIgnore, $default, $bal, $loan\n");
                if ( / reduced by:* / ) {
                    printReduced($time, $seqId, $borrower, $bal, $default);
                } elsif ($default != "") {
                    printDefault($time, $seqId, $borrower, $bal, $default);
                } else {
                    printLoan($time, $seqId, $borrower, $bal, $loan);
                }
        } elsif ( / CouponBank / ) {
            my ($match, $colon, $claims) = / CouponBank - Redeemed (mismatched) Coupons for [^:]*(:(.*\([0-9.]+\),? ?)*)?$/;

            if ($match =~ /mismatched/) {
                print "redemptions weren't equal: $claims\n";
#                printMismatch("", "$claims");
            }
        } elsif (/ Score .*[dD]ividend: /) {
                my ($event, $player, $dividend, $s, $score, $b, $bonus, $p, $paid, $c, $coupons)
                  = ($_ =~ /Score - ([0-9]*)?# ([-\w]*) [dpubprivD]+ividend: ([-0-9.]+)( score: ([-0-9.]+))?( bonus: ([-0-9.]+))?( dividen[Paid]+: ([-0-9.]+))?( [coupgains]+: ([-0-9.]+))?/);
#print("Dividend: $player, $time, $dividend, $score, $bonus, $paid, $coupons\n");
                printDividend($player, $time, $seqId, $dividend, $paid, $coupons);
        } elsif (/ Score .* score: /) {
                my ($event, $player, $score) = ($_ =~ /Score - ([0-9]*)?# ([-\w]*) score: ([0-9.]+)/);
#print("Score: $player, $score\n");
                printScore($player, $event, $score);
        } elsif (/ Score .* dividend: /) {
                my ($ignore, $player, $dividend) = ($_ =~ /Score - ([0-9]*# )?([-\w]*) dividend: ([-0-9.]+)/);
#print("Dividend: $player, $dividend\n");
                saveDividend($player, $dividend);
        } elsif (/ Score .* accepted loan/) {
                my ($borrower, $increase) = ($_ =~ /Score - Borrower ([-\w]*) accepted loan increase of ([-0-9.]+)./);
                printLoanAcceptance($time, $borrower, $increase);
        } elsif (/ Score .*#.* /) {
                my ($event, $player, $rest) = ($_ =~ /Score - ([0-9]*)?# ([-\w]*)(.*)/);
                if ($rest !~ " ") {
print("malformed Score: #$event, $player:         '$rest'\n");
                }
        } elsif (/ Score /) {
                my ($player, $rest) = ($_ =~ /Score - ([-\w]*)(.*)/);
print("malformed score: $player:         '$rest'\n");
        } elsif (/ Transition.*startRound/) {
                $round = $round + 1;
                %book = ( $maxPrice    => "error", "0" => "error" );
                $bestBid = 0;
                $bestAsk = $maxPrice;
#                $sessionType = "EXP"; #JJDM - no need to print this.
#                if ($round <= 1) {
#                    printTitles($sessionType);
#                }
        } elsif (/ Trading /) {
            if ( /liquidate/ ) {
                my ($trader, $issue) = ($_ =~ /Trading - (.*) wants to liquidate (.*)/);
                ($goalAction, $goalIssue, $costLimit, $priceTarget) = ($_ =~ /Trading - .* wants to (liquidate) (.*)/);
            } elsif ( /wants to/ ) {
                ($goalAction, $goalIssue, $costLimit, $priceTarget) = ($_ =~ /Trading - .* wants to (.*) (.*) with limit of (.*) @ (.*)\./);
            } else {
print("found 'Trading' without command.\n");
            }
        } elsif (/ VotingSession /) {
                processVotes($time, $seqId, $_);
        } elsif (/ IndividualTimingEvent.*Judge /) {
            my ($event, $judge) = ($_ =~ /- IndividualTimingEvent - ([0-9]*)?# Judge '(.+)' timer expired/ );
            printJudgeTimedOut($time, $event, $judge);
        } elsif (/ IndividualTimingEvent.*repaying debt/) {
            my ($event, $borrower) = ($_ =~ /- IndividualTimingEvent - ([0-9]*)?# Trader '(.+)' finished repaying debt/ );
            debtRepayment($time, $event, $borrower);
        } elsif (/ IndividualTimingEvent.*debt/) {
                # noop
        } elsif (/ trace /) {
                %book = ( $maxPrice    => "error", "0" => "error" );
                $bestBid = 0;
                $bestAsk = $maxPrice;
                $sessionType = "PM";
                printTitles($sessionType);
        } elsif (/ RPCServer\$RPCHandler /) {
            if (/ Completed grant user /) {
                my ($i, $o, $c) = ($_ =~ /Handler - (.*)# Completed grant user (.*) cash in the amount of: (.*)/);
                printGrantCash($time, $i, $o, $c);
            } elsif (/ Completed create market /) {
                my ($i, $m, $en, $o) = ($_ =~ /Handler - (.*)# Completed create market (.*) endowed with: (.*) on authority of (.*)/ );
                createMarket($time, $i, $o, $m, $en);
            } elsif (/ Completed close market /) {
                my ($time, $i, $m, $p, $o, $r) = ($_ =~ / (..:..:..) +[A-Z]+ -.+Handler - (.*)# Completed close market (.*) in favor of the position: (.*) on authority of (.*). +Redeemed (.*) pairs\./ );
                closeMarket($time, $i, $o, $m, $p, $r);
            } elsif (/ Attempting to create Market / || / Attempting to close Market / || /Completed retrieval of market mak/    ||
                        /Granting cash / || /trade claim for/ ) {
# ignore
            }
        } elsif (/ WARN[ -]*Session - no .* property / || / WARN[ -]*Session - Attempted to reset / || / DEBUG / ) {
# ignore
        } elsif (/CHAT_MESSAGE/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($sender, $recipients, $message) = ($_ =~ /CHAT_MESSAGE: FROM=(.*) TO=\[(.*)\] MSG=(.*)/);
			$message =~ s/^\s*(.*?)\s*$/$1/g; # trim
			$message =~ s/,/~/g; # remove commas
			$message =~ s/\s+/ /g; # consolidate spaces
			$recipients =~ s/^\s*(.*?)\s*$/$1/g; # trim
			$recipients =~ s/,/\//g; # remove commas
			$recipients =~ s/\s+//g; # consolidate spaces
			printAll($time, $lineNumber, $sender, "Chat", "", "", $sender, $recipients, "", "", "", "", "", $message );
		} elsif (/CHAT_BLOCK/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($blocker, $blocked) = ($_ =~ /CHAT_BLOCK: USER=(.*) AFFECTED=(.*)/);
			$blocker =~ s/\s+/ /g; # consolidate spaces
			$blocked =~ s/\s+/ /g; # consolidate spaces
			printAll($time, $lineNumber, $blocker, "Blocked", "", "", $blocker, $blocked, "", "", "", "", "", "" );
        } elsif (/CHAT_UNBLOCK/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($blocker, $unblocked) = ($_ =~ /CHAT_UNBLOCK: USER=(.*) AFFECTED=(.*)/);
			$blocker =~ s/\s+/ /g; # consolidate spaces
			$unblocked =~ s/\s+/ /g; # consolidate spaces
			printAll($time, $lineNumber, $blocker, "Unblocked", "", "", $blocker, $unblocked, "", "", "", "", "", "" );
		} elsif (/CHAT_COLOR_ASSIGNMENT/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($participant, $colors) = ($_ =~ /CHAT_COLOR_ASSIGNMENT FOR (.*): \{(.*)\}/);
			my $cleanedColors = join(';', split(', ', $colors));
			printAll($time, $lineNumber, $participant, "Color Assignment", "", "", "", "", "", "", "", "", "", $cleanedColors );
		} elsif (/PARTICIPATION_CHOICE/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($round, $participant, $participate, $clue, $participationCash) = ($_ =~ /PARTICIPATION_CHOICE: ROUND=(.*) ID=(.*) PARTICIPATE=(.*) CLUE=(.*) PARTICIPATION_CASH=(.*)/);
			printAll($time, $lineNumber, $participant, "Participation Choice", "", "", "", "", "Participate = ${participate}", "Clue = ${clue}", "Participation Cash = ${participationCash}", "", "", "");
		} elsif (/CLUE_ASSIGNMENT/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($round, $participant, $clue, $participationCash) = ($_ =~ /CLUE_ASSIGNMENT: ROUND=(.*) ID=(.*) CLUE=(.*)/);
			printAll($time, $lineNumber, $participant, "Clue Assignment", "", "", "", "", $clue, "", "", "", "", "" );
		} elsif (/ROUND_SCORE_RESULT/ ) {
			my ($lineNumber) = ($_ =~ /ZocaloLogger - (.*)#/);
			my ($participant, $cash, $couponValue, $couponCount, $totalCouponValue, $participationCash, $totalScore) = ($_ =~ /ROUND_SCORE_RESULT: USER=(.*) CASH=(.*) COUPON_VALUE_FOR_ROUND=(.*) COUPON_COUNT=(.*) TOTAL_COUPON_VALUE=(.*) PARTICIPATION_CASH=(.*) TOTAL_SCORE=(.*)/);
			printAll($time, $lineNumber, $participant, "dividend", "", $totalScore, "", "", "Participant = ${participant}; Cash = ${cash}; Coupon Value = ${couponValue}; Coupon Count = ${couponCount}; Total Coupon Value = ${totalCouponValue}; Participation Cash = ${participationCash}; Total Score = ${totalScore}", "", "", "", "", "" );
		} elsif (/NEW_ROUND_STARTED/ ) {
			# ignore
		} else {
			print " MISSED Command: ", $_;
		}
}

sub printBook {
    my ($b);
    foreach $b (keys (%book)) {
        if ($book{"$b"} =~ "error") {
            print " [$b] ";
        } else {
            my $offers =  $book{"$b"};
            print "$b |$offers|; ";
        }
    }
    print "\n";
}

sub distinct {
    my ($market, $position) =@_;

    if ($useDistinctNames) {
        return "$market:$position";
    } else {
        return "$position";
    }
}

sub mmPrice {
    my ($market, $position) =@_;
    return $mmPrice{"$market:$position"};
}

sub setMmPrice {
    my ($market, $position, $newValue) =@_;
    $mmPrice{"$market:$position"} = $newValue;
}

sub printGrantCash {
    my ($time, $event, $user, $cash) =@_;
        printAll($time, $event, $user, "grant cash via RPC", "", $cash, "", "", "", "", "", $sessionType, "", "");
}

sub printLoanAcceptance(time, borrower, increase) {
    my ($time, $borrower, $increase) =@_;
    printAll($time, "", $borrower, "Player accepted loan", "", "", "", "", "", "", "", $sessionType, "", $increase);
}

sub debtRepayment {
    my ($time, $event, $borrower) =@_;
        printAll($time, $event, $borrower, "Borrower Repaid Debt", "", "", "", "", "", "", "", $sessionType, "", "");
}

sub printJudgeTimedOut {
    my ($time, $event, $judge) =@_;
        printAll($time, $event, $judge, "judge timed out", "", "", "", "", "", "", "", $sessionType, "", "");
}

sub createMarket {
    my ($time, $event, $owner, $market, $endowment) =@_;

    $openMarketCount ++;
    if ($openMarketCount > 1) {
        print "creating market ($market) while another is open.\n";
        $useDistinctNames = 1;
    }

    $beta = $endowment / log(2);
    printAll($time, $event, $owner, "create market via RPC", $endowment, "", "", "", "", "", "", $sessionType, $market, "");
}

sub closeMarket {
    my ($time, $event, $owner, $market, $position, $redemptions) =@_;
#print "Closing at $time #$event by $owner, '$market:$position', $redemptions pairs\n";
    $openMarketCount --;
    if ($openMarketCount == 0) {
        $useDistinctNames = 0;
    }
    printAll($time, $event, $owner, "close market via RPC", $redemptions, "", "", "", "", "", "", $sessionType, $market, "");
}

sub printExpCsv {
	my ($filename, $time, $event, $actor, $action, $quant, $price, $buyer, $seller, $n, $o, $p, $q, $r, $s) = @_;
	my $role = $roles{$actor};
	my $roundLogic = $round;
	if($action eq "Participation Choice" || $action eq "Clue Assignment") {
		$roundLogic = $round + 1;
	}
	print "$filename,$time,$event,$roundLogic,$actor,$action,$quant,$price,";
	print "$buyer,$seller,$bestBid,$bestAsk,$role,";
	print "$n,$o,$p,";
	print "$q,$r,$s\n";
}

sub printMakerTrade {
        my ($time, $event, $actor, $action, $quant, $price, $market, $outcome,
                           $goalAct, $goalIssue, $costLimit, $priceTarget, $totalCost) = @_;
        if ($goalAct =~ $actor) {
            printAll($time, $event, $actor, $action, $quant, $price, "", "", "", "", "", $sessionType, $market, $outcome,
                     "continue", $goalIssue, $costLimit, $priceTarget, $totalCost);
            $goalAction = "";
        } else {
            printAll($time, $event, $actor, $action, $quant, $price, "", "", "", "", "", $sessionType, $market, $outcome,
                     $goalAct, $goalIssue, $costLimit, $priceTarget, $totalCost);
            if ($goalAct !~ "") {
                $goalAction = "$actor";  # allow deduction that a transaction occured in two parts
            }
        }
        $goalIssue = $costLimit = $priceTarget = "";
}

sub printOffer {
        my ($time, $event, $actor, $action, $quant, $price, $type) = @_;
        printAll($time, $event, $actor, $action, $quant, $price, "", "", $type, "", "", $sessionType, "", "");
}

sub printRedemption {
        my ($time, $event, $actor, $quant, $price, $market, $position) = @_;
        printAll($time, $event, $actor, "redeemed coupons", $quant, $price, "", "", "", "", "", $sessionType, $market, $position);
}

sub printCloseSell {
        my ($time, $event, $actor, $quant, $price, $buyer, $type) = @_;
        printAll($time, $event, $actor, "Sell", $quant, $price, $buyer, $actor, $type, "", "", $sessionType, "", "");
}

sub printCloseBuy {
        my ($time, $event, $actor, $quant, $price, $seller, $type) = @_;
        printAll($time, $event, $actor, "Buy", $quant, $price, $actor, $seller, $type, "", "", $sessionType, "", "");
}

sub printCancelBuy {
        my ($time, $event, $actor, $quant, $price) = @_;
        printAll($time, $event, $actor, "Cancel Buy Order", $quant, $price, "", "", "", "", "", $sessionType, "", "");
}

sub printCancelSell {
        my ($time, $event, $actor, $quant, $price) = @_;
        printAll($time, $event, $actor, "Cancel Sell Order", $quant, $price, "", "", "", "", "", $sessionType, "", "");
}

sub printEstimate {
        my ($time, $event, $name, $value) = @_;
        printAll($time, $event, $name, "estimate", "", $value, "", "", "", "", "", $sessionType, "", "");
}

sub printLoan {
        my ($time, $event, $borrower, $balance, $loan) = @_;
        printAll($time, $event, $borrower, "Loan Balance", "", $loan, "", "", "", "$balance", "", $sessionType, "", "");
}

sub printDefault {
        my ($time, $event, $borrower, $balance, $default) = @_;
        printAll($time, $event, $borrower, "Defaulted", "", $default, "", "", "", "", "", $sessionType, "", "");
}

sub printReduced {
        my ($time, $event, $borrower, $balance, $reduction) = @_;
        printAll($time, $event, $borrower, "Reduced by", "", $reduction, "", "", "", "", "", $sessionType, "", "");
}

sub printScore {
        my ($player, $event, $score) = @_;
            printAll("", $event, $player, "score", "", $score, "", "", "", "", "", $sessionType, "", "");
}

sub printDividend {
        my ($player, $time, $event, $dividend, $paid, $coupons) = @_;
            printAll($time, $event, $player, "dividend", $coupons, $paid, "", "", "", "", "", $sessionType, "", "");
}

sub printVote {
        my ($time, $event, $player, $outcome) = @_;
        printAll($time, $event, $player, "vote", "", "", "", "", "", "", "", $sessionType, "", $outcome);
}

sub printChoice {
        my ($time, $event, $outcome) = @_;
        printAll($time, $event, "", "chosen", "", "", "", "", "", "", "", $sessionType, "", $outcome);
}

sub getRole {
    my ($actor) = @_;
    return $roles{$actor};
}

sub getRound {
    return $round;
}

sub getBestBid {
    return $bestBid;
}

sub getBestAsk {
    return $bestAsk;
}

sub processVotes {
    my ($time, $event, $command) = @_;
    if ($command =~ / voted for outcome /) {
        my ($player, $outcome) = ($_ =~ /# (.+) voted for outcome (.+)/);
        printVote($time, $event, $player, $outcome);
    } elsif ($command =~ /outcome was chosen: /) {
        my ($ignore, $outcome) = ($_ =~ / The following outcome was chosen: (.+)\((.+)\)/);
        printChoice($time, $event, $outcome);
    } else {
        print "Didn't recognize vote message. '$command'\n";
    }
}

sub redeem {
    my ($time, $seqId, $name, $quant, $price, $market, $position) = @_;
    printRedemption($time, $seqId, $name, $quant, 100 * $price, $market, $position);
}

sub newOffer {
        my ($time, $event, $name, $quant, $price, $command) = @_;
        if ($quant == 0 && $strict) {
                print "newOffer AT ZERO QUANTITY <$name> ($quant, $price) [$command]\n";
                return;
        }
        if ($command =~ "Buy") {
            if ($bestBid < $price) {
                $bestBid = $price;
            } elsif ($betterPriceRequired && $strict) {
                print "BID ACCEPTED THOUGH THE PRICE ($price) WAS NO HIGHER THAN $bestBid.\n";
            }
        } elsif ($command =~ "Sell") {
            if ($bestAsk > $price){
                $bestAsk = $price;
            } elsif ($betterPriceRequired && $strict) {
                print "ASK ACCEPTED THOUGH THE PRICE ($price) WAS NO LOWER THAN $bestAsk.\n";
            }
        }
        addOffer($price, $name);
        printOffer($time, $event, $name, $command, $quant, $price, "");
}

sub makerTrade {
        my ($time, $event, $name, $quant, $price, $market, $outcome) = @_;
        my $direction;

        my $mmPrice = mmPrice($market, $outcome);
#print "mm: $mmPrice, $price, $quant, $event#, $outcome\n";
        if ($price > $mmPrice) {
            $direction = "buy from";
        } else {
            $direction = "sell to";
        }
        if ($quant != 0) {
            my ($totalCost) = sprintf("%.0f", $beta * log((100 - $mmPrice) / (100 - $price)));
            printMakerTrade($time, $event, $name, "$direction Market Maker", $quant, $price, $market, $outcome,
                           $goalAction, $goalIssue, $costLimit, $priceTarget, $totalCost);
        }
        setMmPrice($market, $outcome, $price);
}

sub addOffer {
    my ($p, $n) = @_;
    if (exists($book{$p})) {
        if ($book{"$p"}  =~ "error") {
 print "tried to add a bid at 0 or maxPrice\n";
        } else {
             my $offers = $book{"$p"};
             $offers = "$offers $n";
            $book{"$p"} = "$offers";
            my $tmp = $book{"$p"};
# print "appending offer <$p> [$n], |$offers| $tmp \n";
#printBook();
        }
    } else {
        $book{"$p"} = $n ;
# print "adding offer <$p> [$n] \n";
#printBook();
    }
}

sub removeOffer {
    my ($p) = @_;

    if ($nextOffer eq "") {
 print("TRIED TO REMOVE offer at price $p, but next offer was empty.\n");
        return;
    }

    my $offers =  $book{"$p"};
    my @splitOffers = split(/ /, $offers);
    my $index = 0;

    $index++ until $splitOffers[$index] eq $nextOffer or $index == 10;
    splice(@splitOffers, $index, 1);
    $book{"$p"} = "@splitOffers";
#printBook();
	return $nextOffer;
}

sub accepted {
#printBook();
        my ($time, $event, $name, $quant, $price) = @_;
        if ($price == $bestAsk &&    $quant > 0) {
                my $seller = removeOffer($price);
                newBestAsk($quant, $price);
                printCloseBuy($time, $event, $name, $quant, $price, $seller, "");
        } elsif ($price == $bestBid && $quant < 0) {
                my $buyer = removeOffer($price);
                newBestBid($quant, $price);
                printCloseSell($time, $event, $name, $quant, $price, $buyer, "");
            } elsif ($price == $bestAsk &&    $quant < 0) {
                my $seller = removeOffer($price);
                newBestAsk($quant, $price);
                printCloseBuy($time, $event, $name, -$quant, $price, $seller, "");
            } elsif ($price == $bestBid && $quant > 0) {
                my $buyer = removeOffer($price);
                newBestBid($quant, $price);
                printCloseSell($time, $event, $name, -$quant, $price, $buyer, "");
            } elsif (($maxPrice - $price) == $bestBid && $quant < 0) {
                my $buyer = removeOffer($maxPrice - $price);
                newBestBid($quant, $maxPrice - $price);
                printCloseSell($time, $event, $name, $quant, $maxPrice - $price, $buyer, "");
            } elsif (($maxPrice - $price) == $bestBid && $quant > 0) {
                my $buyer = removeOffer($maxPrice - $price);
                newBestBid($quant, $maxPrice - $price);
                printCloseSell($time, $event, $name, -$quant, $maxPrice - $price, $buyer, "");
            } else {
print "COULDN'T INTERPRET AN ACCEPTED OFFER: n=$name, q=$quant, p=$price\n";
#printBook();
            }
}

sub cancel {
        my ($time, $event, $name, $quant, $price) = @_;
        my $straightOwner = $book{$price};
        my $invertedPrice = $maxPrice - $price;
        my $invertedOwner = $book{$invertedPrice};
#print "CANCEL p=$price, q=$quant, o=$name, b=$book{$price}, b'=$book{$invertedPrice}, s=$straightOwner, i=$invertedOwner, \n";
        my $usePrice = 0;
        if ($price != 50 && $straightOwner =~ $name && $invertedOwner =~ $name) {
            if ($invertedPrice == $bestAsk || $price == $bestAsk) { # HACK!
                $usePrice = $bestAsk;
            } elsif ($invertedPrice < $price) {
                $usePrice = $invertedPrice;
            } else {
                $usePrice = $price;
            }
      print "attempting to cancel order, can't determine which BUY vs. SELL $name, p=$price or p=$invertedPrice,  GUESSING  $usePrice\n";
        } elsif ($invertedOwner =~ $name) {
            $usePrice = $invertedPrice;
        } elsif ($straightOwner =~ $name) {
            $usePrice = $price;
        } else {
            print "DIDN'T FIND A MATCH to cancel.\n";
            return;
        }

        $nextOffer = $name;
        removeOffer($usePrice);

        if ($usePrice >= $bestAsk) {
                newBestAsk($quant, $usePrice);
                printCancelSell($time, $event, $name, $quant, $usePrice);
        }
        if ($usePrice <= $bestBid) {
                newBestBid($quant, $usePrice);
                printCancelBuy($time, $event, $name, $quant, $usePrice);
        }
}

sub newBestBid {
        my ($quant, $price) = @_;
        my $i;
        if ($price >= $bestAsk && $strict) {
            print "BID CROSSING ASK at line ", $., " p=", $price, "\n";
        }
        my $start = max($bestBid, $price);
        for ($i = $start ; $i >= 0; $i--) {
                if (defined($book{$i}) && length($book{$i}) > 0) {
#print "newBestBid |$book{$i}| $i\n";
                        if ($book{$i} =~ "error") {
                                $bestBid = 0;
                        } else {
                                $bestBid = $i;
                        }
                        return;
                }
        }
        $bestBid = 0;
        return;
}

sub newBestAsk {
        my ($quant, $price) = @_;
        my $i;
        if ($price <= $bestBid && $strict) {
            print "ASK CROSSING BID at line ", $., " p=", $price, "\n";
        }
        my $start = min($bestAsk, $price);
        for ($i = $start ; $i <= $maxPrice; $i++) {
                if (defined($book{$i}) && length($book{$i}) > 0) {
#print "newBestAsk |$book{$i}| $i\n";
                        if ($book{$i} =~ "error") {
                                $bestAsk = $maxPrice;
                        } else {
                                $bestAsk = $i;
                        }
                        return;
                }
        }
        $bestAsk = $maxPrice;
        return;
}

sub max {
        my ($first, $second) = @_;
        if ($first < $second) {
                return $second;
        } else {
                return $first;
        }
}

sub min {
        my ($first, $second) = @_;
        if ($first < $second) {
                return $first;
        } else {
                return $second;
        }
}

sub getHint {
        my ($name, $round) = @_;
        my $hintForName = $hints{$name};
        if (defined($hintForName)) {
                my @hintsByRound = split("[,         ]+", $hintForName);
                return $hintsByRound[$round - 1];
        }
        return "";
}

sub getTarget {
        my ($name, $round) = @_;
        my $targetsForName = $targets{$name};
        if (defined($targetsForName)) {
                my @targetsByRound = split("[,         ]+", $targetsForName);
                return $targetsByRound[$round - 1];
        }
        return "";
}

sub handleConfig {
        my ($line) = @_;
        if ( /\.role:/ ) {
                my ($name, $role) = ($line =~ /- .*# ([-\w]*).role: ([-\w]*)/);
                $roles{$name} = $role;
        } elsif ( /sessionTitle/ ) {
                 printTitles($sessionType);
        } elsif ( /\.hint:/ ) {
                my ($name, $hint) = ($line =~ /- ([-\w]*).hint: (.*)/);
                $hints{$name} = $hint;
                incrementClues($name);
        } elsif ( /actualValue:/ ) {
                my ($values) = ($line =~ /- actualValue: (.*)/);
                @actualValues =    split("[,         ]+", $values);
        } elsif ( /\.target: / ) {
                my ($manipulator, $target) = ($line =~ / ([-\w]*)\.target: (.*)/);
                $targets{$manipulator} = $target;
                incrementTargets($manipulator);
        } elsif ( / maxPrice:/ ) {
                my ($max) = ($line =~ / maxPrice: *([-\w]*)/);
                $maxPrice = 0 + $max;
                $bestAsk = $maxPrice;
        } elsif ( /betterPriceRequired: / ) {
                my ($better) = ($line =~ / betterPriceRequired: ([-\w]*)/);
                if ($better =~ /false/) {
                    $betterPriceRequired = 0;
                } else {
                    $betterPriceRequired = 1;
                }
#        } else {
#            print 'MISSED CONFIG: ', $_;
        }
}

sub incrementTargets {
        my ($manipulator) = @_;
        my @targetsByRound = split("[,         ]+", $targets{$manipulator});
        my $i = 0;
        foreach (@targetsByRound) {
                if ($_ > 0) {
                        $posTargets[$i] ++;
                }
                $i++;
        }
}

sub incrementClues {
        my ($name) = @_;
        my @cluesByRound = split("[,         ]+", $hints{$name});
        my $i = 0;
        foreach (@cluesByRound) {
                if ($_ =~ "Plus") {
                        $posClues[$i] ++;
#                } else {
#print 'empty clause in incrementClues(), i=', i;
                }
                $i++;
        }
}

sub cgiHeaders {
    my ($filename) = @_;
        print "Content-type: text/html\n";
        print "\n";
        print "<html><head>\n";
        print "<title>processed log file: $filename</title>\n";
        print "</head><body>\n";
        print "<br>\n";
        print "<table border=1 cellspacing=0 cellpadding=0>\n";
}

sub cgiFooter {
        print "</table>\n";
        print "</body></html>\n";
}

1;
