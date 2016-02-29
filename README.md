# DS1MatrixMixer
An Ndef-oriented matrix mixer class for SC 3.7 using the Livid DS1 with a built-in EQ mode, mute, and master fader.


### Using the class:

Make some Ndefs:
```
(
	Ndef(\noiseSynth, {
		WhiteNoise.ar();
	});

	Ndef(\sinSynth, {
		SinOsc.ar(220);
	});

	Ndef(\soundIn0, {
		SoundIn.ar(0)!2; // if you want to directly monitor a sound input
	});

	// Any other Ndef synths you might want to use...
)
```

Some Ndefs can use the matrix inputs/sends:
```
(
	// All pre-fader sends from the channels can be accessed via a pre-defined Ndef(\busN). For example:

	Ndef(\reverb, {
		var input = Ndef(\bus0).ar; // this means any channel that has its first knob turned up will be fed into this reverb.

		GVerb.ar(input);
	});

	Ndef(\trackingSynth, {
		var freq, hasFreq;
		var input = Ndef(\bus1).ar; // this means any channel that has its second knob turned up will be fed into this pitch tracker.

		# freq, hasFreq = Pitch.kr(input);

		SinOsc.ar(freq)!2;
	});
)
```

Instantiate the matrix mixer using the names of the Ndefs that you want to use.
```
(
m = DS1MatrixMixer.new([
	\reverb,
	\trackingSynth,
	\noiseSynth,
	\sinSynth,
	\soundIn0
]);
)
```

### Outputs:
All synth outputs are summed on channels [0,1]. Each individual synth output is also sent out to an individual SC output channel.

The first synth is output individually on [2,3].<br>
The second synth is output individually on [4,5].<br>
The third synth is output individually on [6,7].<br>
And so on...

### Buttons:
The lower button in each channel mutes the channel (indicated by the button turning red).

The upper button toggles EQ mode which turns the bottom 3 faders of the channel into a high, mid, low EQ (much like you'd find on something like the Mackie 1202). The top two knobs still function as before (EQ mode is indicated by the button turning white).

The right-hand buttons and knobs are not currently in use in my setup.


