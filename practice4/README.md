# Practice

### Practice 4

https://github.com/molchalin/network/tree/main/lesson4

Log into a VPS using SSH:   
`ssh student@<ip_address>`

Allow packets forwarding    
`sudo sysctl -w "net.ipv4.ip_forward = 1"`

Add a new Firewall rule to not have a firewall on port 5022 
`sudo ufv allow 5022`

Make some other changes     
```
sudo sed -i 's/DEFAULT_FORWARD_POLICY="DROP"/DEFAULT_FORWARD_POLICY="ACCEPT"/' /etc/default/ufw
sudo systemctl restart ufw
```

Forward the packets to another machine on a local network       
`sudo iptables -t nat -A PREROUTING -p tcp --dport 5022 -j DNAT --to-destination <IP>:22`

Change the source of the packet that comes from this machine on port <IP>   
`sudo iptables -t nat -A POSTROUTING -p tcp --dport 22 -d <IP> -j MASQUERADE`

Call the machine on a public IP from the host:      
`ssh -p 5022 <PUBLIC_IP>`

Run (make sure the terminal window is big enough)    
`sudo iptraf`   

We can see packet forwarding!