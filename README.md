# CAS-City

To run this project you will need to have repast Simphony installed with its requirements. 
See: https://repast.github.io/  For a guide on how to install it and Java.

When You ahve Repast installed, clone this repository to a suitable location and import it using the import function in the eclipse project manager. (Right click package exporer window -> import -> General -> Existing projects into work space -> select root directory(navigate to where you cloned the project) -> finish.

To run the project click the dropdown next to the green playbutton(run) and select "run configurations", select citySim model and click run. A window should now open and you can click the play button to run the simulation.

With longer runs and a higher population, tha java heap can run out of memory. To fix this: 
Enter run configurations as before, select citysim model, then select the tab called "Arguments" and enter this string into the VM arguments: -Xms512M -Xmx1524M

If there are any issues, please do not hesitate to contact me:

andrfo@stud.ntnu.no 
andrefosvold@gmail.com
