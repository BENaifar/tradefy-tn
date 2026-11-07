package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.AdminPortfolioRow;

import java.util.List;

public interface IPortfolioAdminService {

    /** Every portfolio with its owner and current value. Prices are fetched once for all users. */
    List<AdminPortfolioRow> listPortfolios();
}
