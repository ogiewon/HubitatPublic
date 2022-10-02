/**
 *  Light Usage Table
 *
 *  Copyright 2022 Hubitat, Inc.  All Rights Reserved.
 *
 */

definition(
	name: "Light Usage Table",
	namespace: "hubitat",
	author: "Bruce Ravenel",
	description: "Show Time Usage of Lights",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	if(state.lights == null) state.lights = [:]
	if(state.lightsList == null) state.lightsList = []
	dynamicPage(name: "mainPage", title: "Light Usage Table", uninstall: true, install: true) {
		section {
			input "lights", "capability.switch", title: "Select Lights to Measure Usage", multiple: true, submitOnChange: true, width: 4
			lights.each {dev ->
				if(!state.lights["$dev.id"]) {
					state.lights["$dev.id"] = [start: dev.currentSwitch == "on" ? now() : 0, total: 0]
					state.lightsList += dev.id
				}
			}
			if(lights) {
				if(lights.id.sort() != state.lightsList.sort()) { //something was removed
					state.lightsList = lights.id
					Map newState = [:]
					lights.each{d ->  newState["$d.id"] = state.lights["$d.id"]}
					state.lights = newState
				}
				updated()
				paragraph displayTable()
				input "refresh", "button", title: "Refresh Table", width: 2
				input "reset", "button", title: "Reset Table", width: 2
			}
		}
	}
}

String displayTable() {
	if(state.reset) {
		def dev = lights.find{"$it.id" == state.reset}
		state.lights[state.reset].start = dev.currentSwitch == "on" ? now() : 0
		state.lights[state.reset].total = 0
		state.remove("reset")
	}
	String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
	str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
		"</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
		"<thead><tr style='border-bottom:2px solid black'><th style='border-right:2px solid black'>Light</th><th>Total On Time</th><th>Reset</th></tr></thead>"
	lights.sort{it.displayName.toLowerCase()}.each {dev ->
		int total = state.lights["$dev.id"].total / 1000
		int hours = total / 3600
		total = total % 3600
		int mins = total / 60
		int secs = total % 60
		String devLink = "<a href='/device/edit/$dev.id' target='_blank' title='Open Device Page for $dev'>$dev"
		String reset = buttonLink("d$dev.id", "<iconify-icon icon='bx:reset'></iconify-icon>", "black", "20px")
		str += "<tr style='color:black'><td style='border-right:2px solid black'>$devLink</td>" +
			"<td style='color:${dev.currentSwitch == "on" ? "green" : "red"}'>$hours:${mins < 10 ? "0" : ""}$mins:${secs < 10 ? "0" : ""}$secs</td>" +
			"<td title='Reset Total for $dev' style='padding:0px 0px'>$reset</td></tr>"
	}
	str += "</table></div>"
	str
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = "15px") {
	"<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='color:$color;cursor:pointer;font-size:$font'>$linkText</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

void appButtonHandler(btn) {
	if(btn == "reset") state.lights.each{k, v ->
		def dev = lights.find{"$it.id" == k}
		state.lights[k].start = dev.currentSwitch == "on" ? now() : 0
		state.lights[k].total = 0
	} else if(btn == "refresh") state.lights.each{k, v ->
		def dev = lights.find{"$it.id" == k}
		if(dev.currentSwitch == "on") {
			state.lights[k].total += now() - state.lights[k].start
			state.lights[k].start = now()
		}
	} else state.reset = btn.minus("d")
}

def updated() {
	unsubscribe()
	initialize()
}

def installed() {
}

void initialize() {
	subscribe(lights, "switch.on", onHandler)
	subscribe(lights, "switch.off", offHandler)
}

void onHandler(evt) {
	state.lights[evt.device.id].start = now()
}

void offHandler(evt) {
	state.lights[evt.device.id].total += now() - state.lights[evt.device.id].start
}
