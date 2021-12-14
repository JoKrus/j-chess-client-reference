# j-chess-client-reference
Reference implementation for j-chess-server


I strongly advise not to base your AI on this client since it has the server as a dependency that is not hosted somewhere and uses the server Position class to generate moves.
This class is extremely inefficient for generating moves which will put your AI at a significant disadvantage.
