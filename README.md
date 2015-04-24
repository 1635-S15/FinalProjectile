# FinalProjectile

[s] ask Piazza question about DisplayServer requirements 
[s] ask Shah if she's covering SRS doc in lecture
[s] ask Shah if we need separate requirements for subclasses 

REQUIREMENTS

[ ] add GV requirements to Word template doc
[ ] write requirements for 
	[ ] VehicleController
	    [ ] UserController
	[ ] Simulator
	[ ] Control
	[ ] Projectile

PROJECTILE

[ ] copy over reusable code from GroundVehicle


GROUNDVEHICLE
 
[ ] loosen restrictions on velocity and make everything slower



DISPLAYSERVER

[s] softcode display and simulation coordinate system 
	[ ] display coordinate system 
	[ ] simulation coordinate system defined in Simulator
	    [ ] VC classes access this for wall avoidance
[s] get key press event information and pass it to Simulator
[ ] display scores
[ ] display projectiles
	[ ] projectiles same color as UC
[ ] GV display
	[ ] each UC has indiv. color 
	[ ] FC different color
	[ ] LC different color


USERCONTROLLER
 
[ ] fields for scores
[ ] method for incrementing score counter when things get shot



SIMULATOR

[ ] transition LC into FC when shot
[ ] remove FC when shot second time
[ ] death when crash into wall?
[ ] death after collision with GV?
[ ] scoring system

NETWORKING

[ ] ???
