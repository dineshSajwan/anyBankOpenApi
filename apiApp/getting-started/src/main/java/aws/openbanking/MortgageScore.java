package aws.openbanking;

import java.util.ArrayList;  
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class MortgageScore {

	private static Logger logger = LoggerFactory.getLogger(MortgageScore.class);

	@GET
	@Path("/ping")
	@Produces(MediaType.TEXT_PLAIN)
	public Response ping() {
		logger.info("Ping invoked..");

		return Response.ok().entity("Pong...").build();
	}

	@GET
	@Path("/accounts")
	public List<Account> getAccounts() {
		logger.info("getAccounts");

		List<Account> Accounts = new ArrayList<Account>();

		Account Account1 = new Account(1, "Account 1", "The first Account");
		Account Account2 = new Account(2, "Account 2", "The second Account");
		Account Account3 = new Account(3, "Account 3", "The third Account");

		Accounts.add(Account1);
		Accounts.add(Account2);
		Accounts.add(Account3);

		return Accounts;
	}

	@POST
	@Path("/creditCapability/{customerId}")
	public Integer creditCapability(@PathParam("customerId") int customerId) {
		logger.info("credit Capability for  {}", customerId);

		return 750;
	}



	@PUT
	@Path("/mortgages/{accountId}")
	public Response putAccount(Account updatedAccount, @PathParam("AccountId") int AccountId) {
		logger.info("Update Account Id {} with {}", AccountId, updatedAccount);

		if (!updatedAccount.getId().equals(AccountId)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		return Response.noContent().build();
	}

}
