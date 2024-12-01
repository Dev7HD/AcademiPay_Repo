package ma.dev7hd.userservice.web;

import lombok.AllArgsConstructor;
import ma.dev7hd.userservice.entities.User;
import ma.dev7hd.userservice.repositories.users.UserRepository;
import ma.dev7hd.userservice.services.IClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final IClientService clientService;

    @GetMapping("/users")
    List<User> getUsers(){
        return userRepository.findAll();
    }

    @GetMapping("/users/{id}")
    User getUser(@PathVariable String id){
        return userRepository.findById(id).orElse(null);
    }

    @PostMapping("/users/new")
    User addUser(@RequestBody User user,@RequestParam String password){
        return null;
    }
}
