package citySim;

public class UserMain {
	public UserMain () {};
	public void start () {
		String [] args = new String []{ "C:/Users/andrfo/Documents/Git/CAS-City/Traffic_Main/CitySim.rs"};
		repast.simphony.runtime.RepastMain.main(args);
	}
	public static void main ( String [] args ) {
		UserMain um = new UserMain () ;
		um . start () ;
	}
}

