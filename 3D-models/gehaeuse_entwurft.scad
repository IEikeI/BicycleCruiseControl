
difference(){
    color([0,0,1])
    cube([86,66,46]);  
    translate([6,6,6]){
        color(1,0,0)
        cube([75,55,41]); 
    }
    translate([40,60,20]){
        color(1,0,0)
        cube(center=true,[20,20,20]); 
    }
}

difference(){
    translate([100,0,0]){
        color([0,0,1])
        cube([96,76,21]);
    }
    translate([104,4,07]){
        color([1,0,0])
        cube([88,68,15]);
    }
   translate([148,38,-1]){
        color([1,0,0])
        cube(center = true,[33,33,25]);
   }
}
