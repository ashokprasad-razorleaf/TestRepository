#***********************************************************************
#	gapPreventCreoTypeChange..tcl
#***********************************************************************
#
#	Checks the entered revision to ensure it matches the sequence
# 
#***********************************************************************
#	Ver 	Date		Who		Update description
#
#	1		2020-06-16	PHW		Initial Creation
#
#***********************************************************************

tcl;

proc Debug { sDebugText } {

	global bDebug

	set sDebugPrefix "DEBUG: "
	set sDebugMessage "${sDebugPrefix}${sDebugText}"

	if { $bDebug == "TRUE" } {
		puts $sDebugMessage
	}
}

eval {
	
	#Enable/disable debug output & Auto Commit
	set bDebug FALSE
	set bOutputENV FALSE
	
	set iRetCode 1
	
	Debug "==================================================================================================="
	Debug "Running gapPreventCreoTypeChange.tcl"
	Debug "==================================================================================================="
	#Outputs the available environment variables if the boolean is true
	if { $bOutputENV == "TRUE" } {
		set lENV [ mql list env ]
		Debug "Env: $lENV"
	}
	
	#Retrieve Attribute Values
	set sObjectID [ mql get env OBJECTID ]
	set sType [ mql get env TYPE ]
	set sName [ mql get env NAME ]
	set sRevision [ mql get env REVISION ]
	#set sNewType [ NEWTYPE]

	set sNotification "'$sType' '$sName' '$sRevision' already exists"

	
	Debug "Exiting with code '$iRetCode', notification '$sNotification'"
	
	return -code $iRetCode ".\n\n$sNotification\n"
		
}

