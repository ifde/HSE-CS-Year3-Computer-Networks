# HSE CS Year3 Computer Networks

A repository for the course "Computer Networks"     
Frolov Ivan

Below are some useful notes


# Java

Create a Maven project  
`mvn archetype:generate`    
groupId: Your unique organization ID    
artifactId: The name of your project folder and final JAR (Java Archive filetype)   

Compile the project using   
`mvn compile`

Create the JAR  
`mvn package`

Run it using    
`java -cp target/test-1.0-SNAPSHOT.jar test.App`    
cp - classpath

`mvn exec:java -Dexec.mainClass="test.Server"`  
D stands for Define

-----

Send to a remote server

```
scp -i ~/.ssh/ssh-key-1768992875951 ./target/task1-1.0-SNAPSHOT.jar user@178.154.199.103:/home/user
```

Then run this on a remote

`java -cp target/task1-1.0-SNAPSHOT.jar test.Server`

### WireShark

In Wireshark, interfaces are the network connection points (like your Wi-Fi or Ethernet adapter) or virtual devices (like loopback or VPN tunnels) on your computer that you choose to monitor for capturing live network traffic

When running a clinet and a server locally, choose the Loopback interface (`lo0`)

Then filter using `tcp.port == XXXX`

### Yandex Cloud 

Creating a private-public SSH key pair, used for connection (private is yours)  
`ssh-keygen -t networking -C "Computer Networks"`

It is saved in files `/Users/admin/.ssh/id_ed25519` (private)   
`/Users/admin/.ssh/id_ed25519.pb` (public)

Open the public key in terminal:    
`cat /Users/admin/.ssh/id_ed25519.pub`

Public key is this:     
`ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIIJrgLSTxqZ8C9uOg0QizsE9qS45enl030n47QnOrqAC`

Or download the private-public key pair from Yandex and unzip it:   
`unzip /Users/admin/Downloads/ssh-key-1768992875951.zip -d /Users/admin/.ssh/`

Connect using   
`ssh -i ~/.ssh/ssh-key-1768992875951 user@178.154.199.103`

Note: setting only for me to read the private key worked    
`chmod 600 /Users/admin/.ssh/ssh-key-1768992875951`

To work with VSCode SSH extention update the ~/.ssh/config file:    
```
Host my-networking-server
    HostName 178.154.199.103
    User user
    IdentityFile ~/.ssh/ssh-key-1768992875951
```

Allocate more memory on a VM:   
```
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

Install Java SDK on a VM:   
`sudo apt update`   
`wget https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.deb`  
`sudo apt install ./jdk-25_linux-x64_bin.deb`

`java --version`

Send a compiled Java ARchive (JAR) to the VM:   
```
scp -i ~/.ssh/ssh-key-1768992875951 ./target/task1-1.0-SNAPSHOT.jar user@178.154.199.103:/home/user
```

Run a server:   
`java -cp ./task1-1.0-SNAPSHOT.jar task1.Server`

Run the local client:   
`java -cp target/task1-1.0-SNAPSHOT.jar task1.Client 178.154.199.103 10000 8
 5000 25`

Get the results on a host:
```
scp -i ~/.ssh/ssh-key-1768992875951 user@178.154.199.103:/home/user/result.csv ./
```

Check the memory usage on the remote:   
`ss -nt`


Task1 notes:   
1. Your Server's Receive Buffer is full (4MB), so it stops reading.
2. Your Server is trying to Write a response, but the Client's Receive Buffer is also full because the Client is too busy sending data to read your responses.
3. out.flush() blocks because it can't send the data.
B4. ecause out.flush() is blocked, your single thread never reaches the in.read() line again.

