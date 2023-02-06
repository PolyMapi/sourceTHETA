import requests
import json
import webbrowser
import time
import subprocess

#execute unix command
def runcmd(cmd, verbose = False, *args, **kwargs):

    process = subprocess.Popen(
        cmd,
        stdout = subprocess.PIPE,
        stderr = subprocess.PIPE,
        text = True,
        shell = True
    )
    std_out, std_err = process.communicate()
    if verbose:
        print(std_out.strip(), std_err)
    pass


#start timer
start_time = time.time()

#begin session
response = requests.post( "http://192.168.1.1/osc/commands/execute", json = 
    {
        "name": "camera.startSession",
        "parameters": {}
    }
)

print(response.text + "\n-------------------------")

#get sessionId
response_dict = json.loads(response.text)
sessionId = response_dict["results"]["sessionId"]

#take a picture
def takeOnePicture():
    #get fingerprint
    response = requests.post("http://192.168.1.1/osc/state", json = {})
    fingerprint = json.loads(response.text)["fingerprint"]

    print(response.text + "\n-------------------------")

    #set options
    response = requests.post( "http://192.168.1.1/osc/commands/execute", json = 
        {
            "name": "camera.setOptions",
            "parameters": {
                "sessionId": sessionId,
                "options": {
                    "clientVersion": 2
                }
            }
        }
    )

    print(response.text + "\n-------------------------")

    #take picture
    response = requests.post( "http://192.168.1.1/osc/commands/execute", json = 
        {
            "name": "camera.takePicture"
        }
    )

    #wait image traitment
    newfingerprint = fingerprint
    while (newfingerprint == fingerprint):
        response = requests.post("http://192.168.1.1/osc/checkForUpdates", json =
            {
                "stateFingerprint": fingerprint

            }
        )
        newfingerprint = json.loads(response.text)["stateFingerprint"]


    print(response.text + "\n-------------------------")

    #get URL
    response = requests.post("http://192.168.1.1/osc/state", json = {})
    response_dict = json.loads(response.text)
    image_url = response_dict["state"]["_latestFileUrl"]

    print(response.text + "\n-------------------------")

    #open image in browser
    webbrowser.open(image_url)

    #download image in current directory
    runcmd('wget ' + image_url, verbose = True)


#take n pictures
def takePictures(n) :
    for i in range(0,n):
        takeOnePicture()


def takeQuickPictures(n) :
    #set options
    response = requests.post( "http://192.168.1.1/osc/commands/execute", json = 
        {
            "name": "camera.setOptions",
            "parameters": {
                "sessionId": sessionId,
                "options": {
                    "clientVersion": 2
                }
            }
        }
    )

    # response = requests.post("http://192.168.1.1/osc/state", json = {})
    # print(response.text + "\n-------------------------")
    # lastURL = json.loads(response.text)["_latestFileUrl"]
    # nameId = int(lastURL.split('R')[1].split('.')[0])
    # print("nameId = ", nameId)

    # print(response.text + "\n-------------------------")
    for i in range(0,n):

        # nameId = nameId+1

        #take picture
        response = requests.post( "http://192.168.1.1/osc/commands/execute", json = 
            {
                "name": "camera.takePicture"
            }
        )
        print(response.text + "\n-------------------------")
        time.sleep(1)
    # time.sleep(5)
    # for i in range(0,n):
    #     #download image in current directory
    #     runcmd("wget http://192.168.1.1/files/035344534c303847803aea0cf9010c01/100RICOH/" + nameId + ".JPG", verbose = True)
        


#takePictures(5)
takeQuickPictures(5)

#end timer
end_time = time.time()

print("\n----------------------------------------\n")
print("Total time = " + str(end_time - start_time) + " s")

print("FINISHED")



