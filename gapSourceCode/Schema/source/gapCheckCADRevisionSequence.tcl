#***********************************************************************
#	gapCheckCADRevisionSequence..tcl
#***********************************************************************
#
#	Checks the entered revision to ensure it matches the sequence
# 
#***********************************************************************
#	Ver 	Date		Who		Update description
#
#	1		2020-06-15	PHW		Initial Creation
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

proc outputErrorMessage { lStateValues } {
	
	global sOutputMessage
	
	set lEmptyAttributes [ checkValues $lStateValues ]

	Debug "Returned List of Empty Attributes: $lEmptyAttributes"

	if { $lEmptyAttributes != "" } {
	
		foreach sEmptyValue $lEmptyAttributes {
			append sOutputMessage \10"- ${sEmptyValue}"
			Debug "sOutputMessage: $sOutputMessage" 
		}
		
		mql notice $sOutputMessage
		return 1
	} else {
		return 0
	}
}

eval {

	set sError [ catch {
	
		#Enable/disable debug output & Auto Commit
		set bDebug FALSE
		set bOutputENV FALSE
		
		set iRetCode 0
		
		Debug "==================================================================================================="
		Debug "Running gapCheckCADRevisionSequence.tcl"
		Debug "==================================================================================================="
		#Outputs the available environment variables if the boolean is true
		if { $bOutputENV == "TRUE" } {
			set lENV [ mql list env ]
			Debug "Env: $lENV"
		}
		
		#Retrieve Attribute Values
		set sObjectID [ mql get env OBJECTID ]
		#set sType CATDrawing
		set sType [ mql get env TYPE ]
		#set sName ENV0189314
		set sName [ mql get env NAME ]
		#set sRevision A
		set sRevision [ mql get env REVISION ]
		#set sEvent Promote
		set sEvent [ mql get env EVENT ]
		#set sUser steve
		set sUser [ mql get env USER ]
        set sNewRevision [ mql get env NEWREV ]
        set sPolicy [ mql get env POLICY ] 
        set sCurrentState [ mql get env CURRENTSTATE ]
		
		set sAllowedTypes [ mql get env 1 ]
		set lAllowedTypes [ split $sAllowedTypes , ]
		
		set sNotification ""

		if { $sPolicy != "Versioned Design TEAM Policy" } {	

			if { [ lsearch $lAllowedTypes $sType ] != -1 } {
				
				Debug "Type of '$sType' is allowed. Assessing..."

				Debug "Object '$sType' '$sName' '$sRevision' ($sObjectID) Found."

				set sMajorRev  $sRevision 

				set sNewMajorRev $sNewRevision 

				Debug "Old Major Revision = $sMajorRev,  New Major Revision = $sNewMajorRev"

				if { $sMajorRev > $sNewMajorRev } {
				
					Debug "New Revision '$sNewMajorRev' is less than existing revision '$sMajorRev'"
				
					set sNotification "Revision '$sNewMajorRev' is invalid"
					set iRetCode 1
				
				} else {
					Debug "New Revision '$sNewMajorRev' is greater than existing revision '$sMajorRev'"
				}
				
			} else {
				Debug "Type of '$sType' is not required, will not check for attribute values"
			}
		
		}  else  {
		
		Debug "Skipping as Policy is 'Versioned Design TEAM Policy'"
			
		}	
		

	} sMessage ]
		
	if {$sError} {
		mql notice \10"$sMessage"
		set iRetCode 1
		
	} 	
	
	Debug "Exiting with code '$iRetCode', notification '$sNotification'"
	
	return -code $iRetCode ".\n\n$sNotification\n"
		
}

