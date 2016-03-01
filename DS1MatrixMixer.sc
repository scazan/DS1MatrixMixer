/*
An Ndef-oriented Matrix Mixer for the Livid DS1.

Also contains a fader and EQ on each channel which is enabled by the upper button on each channel.

*/

DS1MatrixMixer {
	var instruments,
		totalNumInstruments = 8, // DS1 has 8 sliders, one slider per instrument
		midiOut;

	*new {| instrumentsArray |
		^super.new.init(instrumentsArray);
	}

	// Most things happen here. Should separate this into different
	init {| instrumentArray |
		var controls,
			muteStates,
			eqStates,
			colors = Dictionary[
				\green -> 127,
				\yellow -> 126,
				\blue -> 63,
				\red -> 31,
				\purple -> 15,
				\cyan -> 7,
				\white -> 3,
				\off -> 0,
			];

		instruments = instrumentArray;

		controls = Array.fill(totalNumInstruments, \amp);
		muteStates = Array.fill(totalNumInstruments, 1);
		eqStates = Array.fill(totalNumInstruments, 0);

		"Loading Matrix mixer...".postln;

		this.createFaders();

		// 8x8 Matrix routings as Ndefs
		instruments.do({|item, i|
			var busNameSymbol = ("bus"++i).asSymbol;
			Ndef(busNameSymbol, { | instr0Send=0, instr1Send=0, instr2Send=0, instr3Send=0, instr4Send=0, instr5Send=0, instr6Send=0, instr7Send=0 |
				var sig = (Ndef(instruments[0]).ar * instr0Send)
				+ (Ndef(instruments[1]).ar * instr1Send)
				+ (Ndef(instruments[2]).ar * instr2Send)
				+ (Ndef(instruments[3]).ar * instr3Send)
				+ (Ndef(instruments[4]).ar * instr4Send)
				+ (Ndef(instruments[5]).ar * instr5Send)
				+ (Ndef(instruments[6]).ar * instr6Send)
				+ (Ndef(instruments[7]).ar * instr7Send);

				sig;
			});

			// Set the input of the synth to be the bus associated with its order so \in.ar can be used
			Ndef(item) <<> Ndef(busNameSymbol);

		});

		// Setup MIDI responders
		MIDIClient.init;
		MIDIIn.connectAll;

		// Try to connect, ignore if there is failure
		{
			midiOut = MIDIOut.newByName("DS1-DS1 MIDI 1","DS1-DS1 MIDI 1");

			this.turnOffLEDs(midiOut);
		}.try({});

		MIDIdef.noteOff(\buttons, { |val, num |
			var button = (( num ) % 2),
			column = (( num ) / 2).asInt;

			// if this is one of the channel buttons
			if(num <= 15, {

				if(val == 0, {
					// If button is the lower channel button treat it as mute
					if(button == 1, {
						var muteState = muteStates[column];
						if(muteState == 1, {
							// Mute is ENABLED if muteState is 1 (we use this as a multiplier)
							muteState = 0;
							midiOut.noteOn(0,num, colors[\red]);
						}, {
							// Mute is DISABLED if muteState is 1 (we use this as a multiplier)
							muteState = 1;
							midiOut.noteOn(0,num, colors[\off]);
						});

						muteStates[column] = muteState;
						Ndef((instruments[column]++"Fader").asSymbol).set(\mute, muteState);
					}, {
						// If button is the upper channel button treat it as an EQ toggle
						if(button == 0, {
							var eqState;

							if(eqStates[column] == 1, {
								eqState = 0;
								midiOut.noteOn(0,num, colors[\off]);
							}, {
								eqState = 1;
								midiOut.noteOn(0,num, colors[\white]);
							});

							eqStates[column] = eqState;

						});
					});
				});

			});
		});

		MIDIdef.cc(\knobsAndSliders, { |val, num |
			var knob = (( num -1 ) % 5),
			column = (( num - 1 ) / 5).asInt;

			if(num == 49, {
				//set master volume
				Server.local.volume = (val/127).ampdb;
			}, {

				// If the column number is great than our columns, we are now in the volume sliders
				if(column >= 8, {
					column = num - 41;

					Ndef((instruments[column]++"Fader").asSymbol).set(controls[column], (val/127).squared );
				}, {

					// If we have the eq toggle then treat as EQ
					if(knob >= 2 && eqStates[column] == 1, {
						var fader = (instruments[column]++"Fader").asSymbol;

						switch(knob,
							2, {
								Ndef(fader).set(\high, val/127);
							},
							3, {
								Ndef(fader).set(\mid, val/127);
							},
							4, {
								Ndef(fader).set(\low, val/127);
							});

						}, {
							// Otherwise use it as matrix routing
							Ndef(("bus"++knob).asSymbol).set(("instr"++column++"Send").asSymbol, val/127);
						});
					});

					//[instruments[column], knob, val].postln;

				});
			});

			"MatrixMixer ready...".postln;
			^this;
	}

	turnOffLEDs {
		// Turn off all LEDs
		{
			0.5.wait;
			25.do({|i|
				midiOut.noteOn(0,i,0);
				// throttle this to make sure it gets 'em all
				0.01.wait;
			});
		}.fork;
	}
	// Helper function to create a fader for an Ndef that conforms to this interface
	createFaders {
		// If we didn't provide all the instruments, fill the rest of the slots with a single "nothing" synth
		if(instruments.size < totalNumInstruments, {
			var instrumentsList = List.newUsing(instruments);

			var numMissingInstruments = totalNumInstruments - instrumentsList.size,
				missingInstrumentOffset = instrumentsList.size - 1;

			numMissingInstruments.do({
				instrumentsList.add(\nothing);
			});

			instruments = instrumentsList.array;
		});

		instruments.do( {| name, i |
			Ndef((name.asString++"Fader").asSymbol, { | amp=0, mute=1, low=0.5, mid=0.5, high=0.5 |
				var out = Ndef(name.asSymbol).ar;

				out = BPeakEQ.ar(out, 80, 1.7, ( ( low+0.0001 )*2 ).ampdb * 3);
				out = BPeakEQ.ar(out, 2500, 1.7, ( ( mid+0.0001 )*2 ).ampdb * 3);
				out = BPeakEQ.ar(out, 12000, 1.7, ( ( high+0.0001 )*2 ).ampdb * 3);

				Limiter.ar(out, 0.98, 0.01) * amp * mute;
			});
			Ndef((name.asString++"Fader").asSymbol).playN([0,1, (i+1)*2, ((i+1)*2)+1]);
		});
	}

	// Update the instruments array and re-instantiate faders
	update { | instrumentArray |
		instruments = instrumentArray;

		this.createFaders();
	}

	// IN PROGRESS: Show a gui for the DS1 for visual feedback but more importantly for disconnected prototyping
	// TODO: Connect it to MIDI
	// TODO: Add normal control mode or separate that into another class
	gui {
		var window;
		var hLayout; 
		var channels;

		window = Window.new("Livid DS1");
		hLayout = HLayout();
		channels = List.new();
		// Create all channels
		8.do({
			var vLayout = VLayout();
			var controls = List.fill(5, {Knob.new()});

			controls.add(Button.new().states_( [["", Color.white, Color.black], ["", Color.black, Color.cyan]]));
			controls.add(Button.new().states_( [["", Color.white, Color.black], ["", Color.black, Color.red]]));

			controls.add(Slider.new());

			controls.do({|item, i|
				var stretch = 0.8;

				if(i >= 5, {stretch = 3.0});

				vLayout.add(item, stretch);
			});

			hLayout.add(vLayout);
			// Add to the array of controls
			channels.add(controls);
		});

		window.layout = hLayout;

		window.background = Color.black;
		window.front;
		window.onClose = {};

		// Returns the an array for each channel strip with all UI elements so that one can assign actions to them
		^channels;
	}
}
