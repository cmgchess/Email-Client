import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.mail.*;
import java.io.*;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;
import java.time.LocalDateTime;

public class Email_Client {

    public static void main(String[] args) {

        Client a=new Client();
        MyBlockingQueue queue = new MyBlockingQueue(100);
        EmailStatRecorder emailStatRecorder = new EmailStatRecorder();
        EmailStatPrinter emailStatPrinter = new EmailStatPrinter();
        EmailConsumer emailConsumer = new EmailConsumer(queue);
        emailConsumer.addObserver(emailStatRecorder);
        emailConsumer.addObserver(emailStatPrinter);

        EmailReceive emailReceive = new EmailReceive(queue);
        //login using name and password
        a.login();
        //automatically send birthday wishes when client runs
        try{
            a.readFile();
            for(Recipient i: a.getRecipientArrayList()){
                if(i instanceof Checkable) {
                    if(a.checkBirthday(((Checkable) i).getBirthday())){
                        System.out.println("Sending birthday wish....");
                        a.sendEmail(i.getEmail(),"Birthday Wish",((Checkable) i).birthdayWish());
                    }
                }
            }

        }
        catch (Exception e){
            System.out.println(e);
        }

        emailReceive.start();
        emailConsumer.start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter option type: \n"
                + "1 - Adding a new recipient\n"
                + "2 - Sending an email\n"
                + "3 - Printing out all the recipients who have birthdays\n"
                + "4 - Printing out details of all the emails sent\n"
                + "5 - Printing out the number of recipient objects in the application\n"
                + "6 - Printing out the details of the received emails");
        while (true) {
            int option = scanner.nextInt();

            switch (option) {
                case 1:
                    // code to add a new recipient
                    String line;
                    scanner.nextLine();
                    while (!(line = scanner.nextLine()).equals("-1")) {
                        a.writeFile(line);
                    }
                    a.readFile();
                    break;
                case 2:
                    // input format - email, subject, content
                    // code to send an email
                    scanner.nextLine();
                    String details = scanner.nextLine();
                    String[] detailArr = details.split(",", 3);
                    String recipientEmail = detailArr[0];
                    String emailSubject = detailArr[1];
                    String emailContent = detailArr[2];
                    a.sendEmail(recipientEmail, emailSubject, emailContent);

                    break;
                case 3:
                    // input format - yyyy/MM/dd (ex: 2018/09/17)
                    // code to print recipients who have birthdays on the given date
                    scanner.nextLine();
                    String date = scanner.nextLine();
                    for (Recipient i : a.getRecipientArrayList()) {
                        if (i instanceof Checkable) {
                            if (a.dateCompare(date, ((Checkable) i).getBirthday())) {
                                System.out.println("Name: " + i.getName() + "    Birthday: " + ((Checkable) i).getBirthday());
                            }
                        }
                    }
                    break;
                case 4:
                    // input format - yyyy/MM/dd (ex: 2018/09/17)
                    // code to print the details of all the emails sent on the input date
                    scanner.nextLine();
                    String dateDetails = scanner.nextLine();
                    a.getEmailDetails(dateDetails);
                    break;
                case 5:
                    // code to print the number of recipient objects in the application
                    System.out.println("There are " + Recipient.getCount() + " recipients");
                    break;
                case 6:
                    //code to print received email details
                    emailReceive.getReceivedDetails();
                    break;
            }
        }
    }
}
class Client{
    private static String user;
    private static String password;
    private ArrayList<Recipient> recipientArrayList= new ArrayList<Recipient>();
    private static ArrayList<EmailDetails> detailsArrayList = new ArrayList<EmailDetails>();
    Client(){
        try{
            File recipients = new File("clientList.txt");
            if (!recipients.exists()){
                recipients.createNewFile();
                System.out.println("Recipients file has been created");
            }
            File serialize = new File("sentEmails.ser");
            if (!serialize.exists()){
                serialize.createNewFile();
                System.out.println("Serializable file has been created");
                try {
                    FileOutputStream fop=new FileOutputStream("sentEmails.ser");
                    ObjectOutputStream oos=new ObjectOutputStream(fop);
                    oos.writeObject(detailsArrayList);

                }
                catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
        catch (Exception e){
            System.out.println("Error when creating file");
        }

    }
    //method to login
    public void login(){
        System.out.println("Enter login details");
        Scanner senderDetails = new Scanner(System.in);
        System.out.print("Enter your email address: ");
        user = senderDetails.nextLine();
        System.out.print("Enter your password: ");
        password = senderDetails.nextLine();
    }
    //method to write to a file
    public void writeFile(String s){
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter("clientList.txt", true));
            out.write(s);
            out.newLine();
            out.close();
        }
        catch (Exception e) {
            System.out.println("exception occurred");
        }
    }
    //method to read from a file and separate into different recipient objects
    public void readFile(){
        try{
            Scanner read = new Scanner(new File("clientList.txt"));
            read.useDelimiter("[:,\n]");
            while (read.hasNext()){
                switch (read.next()) {
                    case "Official": {
                        String name = read.next();
                        String email = read.next();
                        String designation = read.next();
                        recipientArrayList.add(new Official(name,email,designation));
                        break;
                    }
                    case "Office_friend": {
                        String name = read.next();
                        String email = read.next();
                        String designation = read.next();
                        String birthday = read.next();
                        recipientArrayList.add(new OfficeFriend(name,email,designation,birthday));
                        break;
                    }
                    case "Personal": {
                        String name = read.next();
                        String nickname = read.next();
                        String email = read.next();
                        String birthday = read.next();
                        recipientArrayList.add(new Personal(name,email,nickname,birthday));
                        break;
                    }
                }
            }

        }
        catch (Exception e){
            System.out.println("Error");
        }

    }
    //method to get array list of recipient objects
    public ArrayList<Recipient> getRecipientArrayList(){
        return recipientArrayList;
    }
    //method to send emails
    public void sendEmail(String recipientEmail,String emailSubject,String emailContent){
        String host = "smtp.gmail.com";

        Properties properties = new Properties();
        properties.put("mail.smtp.host",host);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user,password);
                    }
                });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.addRecipient(Message.RecipientType.TO,new InternetAddress(recipientEmail));
            message.setSubject(emailSubject);
            message.setText(emailContent);

            Transport.send(message);
            System.out.println("Message sent successfully");
            EmailDetails detail = new EmailDetails(getTodayDate(),recipientEmail,emailSubject);
            deserialization(detail);
        }
        catch (Exception e){
            System.out.println("Exception: "+e);
        }
    }
    //method to check if current date is a birthday
    public boolean checkBirthday(String birthday){
        DateTimeFormatter date = DateTimeFormatter.ofPattern("MM/dd");
        LocalDateTime now = LocalDateTime.now();
        return (date.format(now).trim()).equals(birthday.substring(5));
    }
    //method to check if a given date is a birthday
    public boolean dateCompare(String date,String birthday){
        return (date.substring(5)).equals(birthday.substring(5));
    }
    //method to get current date
    public String getTodayDate(){
        DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime now = LocalDateTime.now();
        return date.format(now).trim();
    }
    //method to serialize
    public void serialization(ArrayList<EmailDetails> detailsArrayList){
        try {
            FileOutputStream fop=new FileOutputStream("sentEmails.ser");
            ObjectOutputStream oos=new ObjectOutputStream(fop);
            oos.writeObject(detailsArrayList);

        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    //method to deserialize, add new object after that again serialize
    public void deserialization(EmailDetails details){
        try{
            FileInputStream fis=new FileInputStream("sentEmails.ser");
            ObjectInputStream ois=new ObjectInputStream(fis);
            ArrayList<EmailDetails> deserDetails = (ArrayList<EmailDetails>)ois.readObject();
            deserDetails.add(details);
            serialization(deserDetails);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }
    //method to get the details of a sent email
    public void getEmailDetails(String date){
        try{
            FileInputStream fis=new FileInputStream("sentEmails.ser");
            ObjectInputStream ois=new ObjectInputStream(fis);
            ArrayList<EmailDetails> deserDetails = (ArrayList<EmailDetails>)ois.readObject();
            for(EmailDetails emailDetails: deserDetails){
                if(date.trim().equals(emailDetails.getDate())){
                    System.out.println("Date: "+emailDetails.getDate()+"  Recipient: "+emailDetails.getEmailRecipient()+"  Subject: "+emailDetails.getSubject());
                }
            }
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public static String getUsername(){
        return user;
    }
    public static String getPassword(){
        return password;
    }


}
//class to create serializable email detail objects
class EmailDetails implements Serializable{
    private String date;
    private String emailRecipient;
    private String subject;
    EmailDetails(String date,String emailRecipient,String subject){
        this.date=date;
        this.emailRecipient=emailRecipient;
        this.subject=subject;
    }

    public String getDate() {
        return date;
    }

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public String getSubject() {
        return subject;
    }
}

abstract class Recipient{
    private String name;
    private String email;
    private static int count=0;
    public Recipient(String name,String email){
        count++;
        this.name=name;
        this.email=email;
    }
    public String getName(){
        return name.trim();
    }
    public String getEmail(){
        return email.trim();
    }
    //static count to get the no of recipients
    public static int getCount(){
        return count;
    }
}

class Official extends Recipient{
    private String designation;
    Official(String name, String email, String designation){
        super(name,email);
        this.designation=designation;
    }
}

class Personal extends Recipient implements Checkable{
    private String nickname;
    private String birthday;
    Personal(String name,String email,String nickname,String birthday){
        super(name,email);
        this.nickname=nickname;
        this.birthday=birthday;
    }
    public String getBirthday(){
        return birthday.trim();
    }
    public String birthdayWish(){
        return "hugs and love on your birthday. Chathulanka";
    }

}

class OfficeFriend extends Official implements Checkable{
    private String birthday;
    OfficeFriend(String name, String email, String designation,String birthday){
        super(name,email,designation);
        this.birthday=birthday;
    }
    public String getBirthday(){
        return birthday.trim();
    }
    public String birthdayWish(){
        return "Wish you a Happy Birthday. Chathulanka";
    }

}

interface Checkable{
    String getBirthday();
    String birthdayWish();
}


//-------------------------------below are related for receiving emails---------------------------------------------------------------
//class to write to console
class EmailStatRecorder implements Observer {
    private ArrayList<ReceivedEmailDetails> tempMessages;

    EmailStatRecorder(){
        tempMessages = new ArrayList<ReceivedEmailDetails>();
    }

    //can write received email details to console directly using this method
    public void writeConsole(){
        for (ReceivedEmailDetails receivedEmailDetails: tempMessages){
            System.out.println("Date:  "+receivedEmailDetails.getDate().toString());
            System.out.println("From:  "+receivedEmailDetails.getFrom());
            System.out.println("Subject:  "+receivedEmailDetails.getSubject());
            System.out.println("Content:  \n"+receivedEmailDetails.getContent());
            System.out.println();
        }
        if (tempMessages.isEmpty())System.out.println("No new emails received");
        else tempMessages = new ArrayList<ReceivedEmailDetails>();
    }

    @Override
    public void update(ReceivedEmailDetails receivedEmailDetails) {
        System.out.println("an email is received at "+java.util.Calendar.getInstance().getTime());
        tempMessages.add(receivedEmailDetails);
        //writeConsole();
    }
}

//class to write to files
class EmailStatPrinter implements Observer {

    private static ArrayList<ReceivedEmailDetails> receivedArrayList = new ArrayList<ReceivedEmailDetails>();

    EmailStatPrinter(){
        File notification  = new File("notification.txt");
        if (!notification.exists()){
            try {
                notification.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating notification file");
                ;
            }
            System.out.println("Notifications file has been created");
        }
        File serialize = new File("receivedEmails.ser");
        if (!serialize.exists()){
            try {
                serialize.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating serializable file");
            }
            System.out.println("Received Email details Serializable file has been created");
            try {
                FileOutputStream fop=new FileOutputStream("receivedEmails.ser");
                ObjectOutputStream oos=new ObjectOutputStream(fop);
                oos.writeObject(receivedArrayList);

            }
            catch (Exception e) {
                System.out.println(e);
            }
        }

    }

    //method to write to text file
    public void writeFile(){
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter("notification.txt", true));
            out.write("an email is received at "+java.util.Calendar.getInstance().getTime());
            out.newLine();
            out.close();
        }
        catch (Exception e) {
            System.out.println("exception occurred");
        }
    }
    //method to serialize
    public void serialization(ArrayList<ReceivedEmailDetails> receivedArrayList){
        try {
            FileOutputStream fop=new FileOutputStream("receivedEmails.ser");
            ObjectOutputStream oos=new ObjectOutputStream(fop);
            oos.writeObject(receivedArrayList);

        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    //method to deserialize, add new object after that again serialize
    public void deserialization(ReceivedEmailDetails receivedEmailDetails){
        try{
            FileInputStream fis=new FileInputStream("receivedEmails.ser");
            ObjectInputStream ois=new ObjectInputStream(fis);
            ArrayList<ReceivedEmailDetails> deserRecDetails = (ArrayList<ReceivedEmailDetails>)ois.readObject();
            deserRecDetails.add(receivedEmailDetails);
            serialization(deserRecDetails);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }




    @Override
    public void update(ReceivedEmailDetails receivedEmailDetails) {
        writeFile();
        deserialization(receivedEmailDetails);

    }
}

//this is producer
class EmailReceive extends Thread{
    private MyBlockingQueue queue;
    EmailReceive(MyBlockingQueue queue){
        this.queue=queue;
    }
    public void check(String username, String password){
        String host = "pop.gmail.com";
        try{
            Properties properties = new Properties();
            properties.put("mail.pop3.host", host);
            properties.put("mail.pop3.port", "995");
            properties.put("mail.pop3.starttls.enable","true");
            Session emailSession = Session.getDefaultInstance(properties);

            Store store = emailSession.getStore("pop3s");
            store.connect(host, username, password);

            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message messages[] = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message msg: messages){
                Date date = msg.getSentDate();
                String from = msg.getFrom()[0].toString();
                String subject = msg.getSubject();
                String content = getTextFromMessage(msg);
                ReceivedEmailDetails temp = new ReceivedEmailDetails(date,from,subject,content);
                msg.setFlag(Flags.Flag.SEEN, true);

                queue.enQueue(temp);

            }
            emailFolder.close();
            store.close();
        }
        catch (Exception e) {
            System.out.println("Error receiving Email");
        }
    }
    //below two methods needed to get the content from an email
    //https://stackoverflow.com/questions/11240368/how-to-read-text-inside-body-of-mail-using-javax-mail
    private  String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private  String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }

    public void run(){
        while(true){
            try{
                check( Client.getUsername(),Client.getPassword());
            }
            catch(Exception ex){ // if an error occurs this thread will automatically closes it self.
                System.out.println("Error occurred");

            }
        }
    }
    //method to obtain the received email details from the serialized file
    public void getReceivedDetails(){
        try{
            FileInputStream fis=new FileInputStream("receivedEmails.ser");
            ObjectInputStream ois=new ObjectInputStream(fis);
            ArrayList<ReceivedEmailDetails> deserReceivedDetails = (ArrayList<ReceivedEmailDetails>)ois.readObject();
            for(ReceivedEmailDetails receivedEmailDetails: deserReceivedDetails){
                System.out.println("Date:  "+receivedEmailDetails.getDate().toString());
                System.out.println("From:  "+receivedEmailDetails.getFrom());
                System.out.println("Subject:  "+receivedEmailDetails.getSubject());
                System.out.println("Content:  \n"+receivedEmailDetails.getContent());
                System.out.println("----------------------------------------------------------------------------------------------");
            }
        }
        catch (Exception e){
            System.out.println(e);
        }
    }


}
//this class removes emails from the blocking queue. this is the consumer and also observable
class EmailConsumer extends Thread implements Observable{
    private MyBlockingQueue queue;
    private Observer[] observers;
    private int index;

    EmailConsumer(MyBlockingQueue queue){
        this.queue=queue;
        observers = new Observer[2];
        index = 0;
    }
    //method to add observer to observer list
    @Override
    public void addObserver(Observer observer) {
        observers[index++]=observer;
    }
    //method to notify all the observers the state change
    @Override
    public void notifyObservers(ReceivedEmailDetails receivedEmailDetails) {
        for(Observer observer : observers){
            observer.update(receivedEmailDetails);
        }
    }

    public void run(){
        while (true){
            notifyObservers(queue.deQueue());
        }
    }
}
//custom implemented blocking queue
class MyBlockingQueue {
    private Queue<ReceivedEmailDetails> blockingQ;
    private int maxSize;
    MyBlockingQueue(int maxSize){
        this.maxSize=maxSize;
        blockingQ = new LinkedList<ReceivedEmailDetails>();
    }

    public synchronized void enQueue(ReceivedEmailDetails obj){
        while (blockingQ.size()==maxSize){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        blockingQ.add(obj);
        notifyAll();
    }

    public synchronized ReceivedEmailDetails deQueue(){
        while (blockingQ.size()==0){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        notifyAll();
        return blockingQ.remove();
    }


    public Queue getBlockingQueue(){
        return blockingQ;
    }

}
//class which creates the received email detail objects
class ReceivedEmailDetails implements Serializable {
    private Date date;
    private String from;
    private String subject;
    private String content;
    ReceivedEmailDetails(Date date, String from, String subject, String content){
        this.date=date;
        this.from=from;
        this.subject=subject;
        this.content=content;
    }

    public Date getDate(){
        return date;
    }
    public String getFrom(){
        return from;
    }
    public String getSubject(){
        return subject;
    }

    public String getContent() {
        return content;
    }
}
//interface which contains the abstract methods related to observable
interface Observable {
    public void addObserver(Observer observer);
    public void notifyObservers(ReceivedEmailDetails receivedEmailDetails);
}
//interface which contains the abstract methods related to observer
interface Observer {
    public void update(ReceivedEmailDetails receivedEmailDetails);
}
