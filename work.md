# todo 

##  Programming 
- Fix client thread / how do we want to handle file requests? 
- Possibly another API endpoint? (usable by application, for example) 
- Extensions
        ^ Cache
        * Ultrapeers
        * Improve search efficiency
        Dynamic querying
        Querying routing protocol
        Push

##  Testing
- Add more debugging statements 
- Come up with 5 test topologies, Helpful with debugging too 
  - See test/help.txt
- Tester class
  - -t1, -t2, ... -t5, -all flags passed into program to 
  - run each test 
  - Print output and if passed/failed 
        
    
##  Writeup / documentation 
- Overview of protocol 
- Explanation of why this is time-consuming
- Design & Implementation
- Extensions (if applicable) 
- Testing environment description, which machines used etc
   - overview of test commands 
- Test cases
   - Topologies represented graphically? 
- Results  

-------------------------------------------------------------------------
# Done 

Classes
    Peer
    GnutellaPacket (multiplexing and demultiplexing for query/hitquery messages)
        changes structure -> byte[]
        constructs Query message  (message ID, TTL, file name)
        constructs Hitquery message  (message ID, TTL, file name, peer IP, port number)
    AssociativeArray - [message ID, upstream peer ID]
        flushing policy

Peer state
    int peer ID
    DATA STRUCTURE Message IDs + upstream peers
    DATA STRUCTURE local storage / cache
        can be just files stored on machine
    IMMUTABLE DATA STRUCTURE list of neighbors

File requesting
    go through list of files to get
    form queries for all of them
    send them off
    timeouts?

Query handling - query()
    checking that the message hasn't already been seen
    updating the associative array
    searching local storage
        if file found, sends hitquery message upstream (using hitquery handling function)
    decrement TTL, (increment hops field)
    if TTL is not 0 & not seen already, forwarding to neighbors using TCP socket connection

Hitquery handling - hitquery()
    searching the associative array
    if not something the peer sent,
        sends hitquery message upstream
    otherwise, open connection to the appropriate port and download the file

File request handling - filerequest()
    specify the port
    listen for connections at that port
    HTTP protocol - file index, file size, file name

Multithreading
    Client-side
    HTTP port - 5760
        upon reception of TCP connection request, accepting TCP connections from upstream for files peer has
    Queries port - 7777
        upon reception of hitquery message, propagating responses back to original sender
        upon reception of query message, sending to neighbors as described above
