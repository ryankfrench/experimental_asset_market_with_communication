# Used to turn CSV output in JS output for a replay file.

# Target Output
# scheduleAddSell("0:08", 55);
# scheduleAccept("0:11", 55);

# Possible actions in JS
# scheduleAccept
# scheduleAddBuy
# scheduleAddSell
# scheduleCancel

package jjdm;

use strict;
use feature qw(say);
use POSIX;

my $filename = @ARGV[0];
my $target_round = @ARGV[1];
my $participant = @ARGV[2];

die "Usage: perl csvToReplay.pl <path_to_csv_file> <target_round>" unless ($filename && $target_round);
die "The file does not exist ($filename)." unless (-e $filename);

say "Using Filename: [$filename] -  target_round: [$target_round]";

open(CSVFILE, $filename);
my $line = 1;
my @lines=<CSVFILE>;
close(CSVFILE);

my @actions;

foreach(@lines) {
	next if($line++ == 1);
	my @parts = split(',', $_);
	my $time = @parts[1];
	my $round = @parts[3];
	my $actor = @parts[4];
	my $action = @parts[5];
	my $price = @parts[7];
	next if($round != $target_round);
	
	# RULES FOR TURNING CSV INTO JS
	# added Buy         = scheduleAddBuy
	# added Sell        = scheduleAddSell
	# Buy               = scheduleAccept
	# Sell              = scheduleAccept
	# Cancel Buy Order  = scheduleCancel
	# Cancel Sell Order = scheduleCancel
	# dividend          = [NOTHING]
	
	my $jsAction = undef;
	if ($action =~ /added ([a-zA-Z]+)/) {
		$jsAction = ('Buy' eq $1) ? 'scheduleAddBuy' : 'scheduleAddSell';	
	} elsif ($action eq 'Buy' || $action eq 'Sell') {
		$jsAction = 'scheduleAccept';	
	} elsif ($action =~ /Cancel/) {
		$jsAction = 'scheduleCancel';	
	}
		
	push @actions, [ $jsAction, $time, $price, $actor ] if $jsAction;
	#say "$action at $price - $1 [$jsAction]";
	
}

# create relative time.
# scheduleAddSell("0:08", 55);
# "mm:ss" since first time.

sub getTimeFromString {
	my ($timeString) = @_;
	my ($hh, $mm, $ss, $ms) = ($timeString =~ /(\d+):(\d+):(\d+)\.(\d+)/);	
	return ($hh * 60 * 60) + ($mm * 60) + $ss;
}	

sub getTimeOffset {
	my ($stringTime, $offsetInSeconds) = @_;
	my $currentTime = getTimeFromString($stringTime);
	my $elapsedTime = $currentTime - $offsetInSeconds;
}

sub secondsToMmSs {
	my ($seconds) = @_;
	my $minute = floor($seconds / 60);
	my $second = $seconds - ($minute * 60);
	return sprintf("%02d", $minute) . ':' . sprintf("%02d", $second);
}

my $firstTime = $actions[0][1];
my $baseTime = getTimeFromString($firstTime);
my $offset = $baseTime - 2; # subtract this from every number

foreach my $parts (@actions) {
	my $thisOffset = getTimeOffset(@$parts[1], $offset);
	my $thisDisplay = secondsToMmSs($thisOffset);
	#say "@$parts[0]\t@$parts[1]\t@$parts[2]\t@$parts[3]\t$thisOffset\t$thisDisplay";
	#scheduleAddSell 07:02:53.317    325     a12     259     04:19
	say "@$parts[0]\(\"$thisDisplay\", @$parts[2]\);";
}