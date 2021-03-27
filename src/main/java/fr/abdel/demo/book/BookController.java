package fr.abdel.demo.book;

import fr.abdel.demo.borrow.Borrow;
import fr.abdel.demo.borrow.BorrowRepository;
import fr.abdel.demo.configuration.MyUserDetailService;
import fr.abdel.demo.user.User;
import fr.abdel.demo.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    //Affichage
    @GetMapping(value = "/books")
    public ResponseEntity listBooks(@RequestParam(required = false) BookStatus status, Principal principal){


        Integer userConnectedId = this.getUserConnectedId(principal);
        List<Book> books;

        //Livres Disponibles
        if(status!=null && status==BookStatus.FREE){
        books = bookRepository.findByStatusAndUserIdNotAndDeletedFalse(status, userConnectedId);
        }

        else{
        //My Books
         books = bookRepository.findByUserIdAndDeletedFalse(userConnectedId);
        }
        return new ResponseEntity(books, HttpStatus.OK);
    }


    public static Integer getUserConnectedId(Principal principal) {

        if(! (principal instanceof UsernamePasswordAuthenticationToken)){
            throw new RuntimeException(("User not found"));
        }
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        Integer userId = ((MyUserDetailService.UserPrincipal)token.getPrincipal()).getUser().getId();

    return userId;
    }

    //Création
    @PostMapping(value = "/books")
    public ResponseEntity addBook(@Valid @RequestBody Book book, Principal principal){


        Integer userConnectedId = this.getUserConnectedId(principal);
        Optional<User> user = userRepository.findById(userConnectedId);
        Optional<Category> category = categoryRepository.findById(book.getCategoryId());


        if(category.isPresent()){
            book.setCategory(category.get());
        }else {
            return new ResponseEntity("You must provide a valid category", HttpStatus.BAD_REQUEST);
        }


        if(user.isPresent()){
        book.setUser(user.get());
        }else{
            return new ResponseEntity("You must provide a valid user", HttpStatus.BAD_REQUEST);
        }

        book.setDeleted(false);
        book.setStatus(BookStatus.FREE);
        bookRepository.save(book);

        return new ResponseEntity(book, HttpStatus.CREATED);
    }



    @DeleteMapping(value = "/books/{bookId}")
    public ResponseEntity deleteBook(@PathVariable("bookId") String bookId, Principal principal){

        Optional<Book> bookToDelete = bookRepository.findById(Integer.valueOf(bookId));

        if(!bookToDelete.isPresent()){
            return new ResponseEntity("Book not found", HttpStatus.BAD_REQUEST);
        }

        Book book = bookToDelete.get();
        List<Borrow> borrows = borrowRepository.findByBookId(book.getId());

        for(Borrow borrow: borrows){
            if(borrow.getCloseDate()==null){
                User borrower = borrow.getBorrower();
                return new ResponseEntity(borrower, HttpStatus.CONFLICT);
            }
        }
        book.setDeleted(true);
        bookRepository.save(book);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }



    @PutMapping(value = "/books/{bookId}")
    public ResponseEntity updateBook(@PathVariable("bookId") String bookId, @Valid @RequestBody Book book, Principal principal){

        Optional<Book> bookToUpdate = bookRepository.findById(Integer.valueOf(bookId));

        if(!bookToUpdate.isPresent()){
            return new ResponseEntity("Book not existing", HttpStatus.BAD_REQUEST);
        }

        Book bookToSave = bookToUpdate.get();
        Optional<Category> newCategory = categoryRepository.findById(book.getCategoryId());
        bookToSave.setCategory(newCategory.get());
        bookToSave.setTitle(book.getTitle());
        bookRepository.save(bookToSave);

        return new ResponseEntity(bookToSave, HttpStatus.OK);
    }


    @GetMapping(value = "/categories")
    public ResponseEntity listCategories(){

        return new ResponseEntity(categoryRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/books/{bookId}")
    public ResponseEntity loadBook(@PathVariable("bookId") String bookId){
        Optional<Book> book = bookRepository.findById(Integer.valueOf(bookId));
        return new ResponseEntity(book.get(), HttpStatus.OK);
    }
}
